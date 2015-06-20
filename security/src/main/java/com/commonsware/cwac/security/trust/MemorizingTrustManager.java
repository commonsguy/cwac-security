/***
  Copyright (c) 2014 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.commonsware.cwac.security.trust;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Implementation of a memorizing trust manager, inspired by
 * https://github.com/ge0rg/MemorizingTrustManager, but
 * designed to be used by TrustManagerBuilder.
 */
public class MemorizingTrustManager implements X509TrustManager {
  private KeyStore keyStore=null;
  private Options options=null;
  private X509TrustManager storeTrustManager=null;
  private KeyStore transientKeyStore=null;
  private X509TrustManager transientTrustManager=null;

  /**
   * @param options
   *          a MemorizingTrustManager.Options object, to
   *          configure the memorization behavior
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws FileNotFoundException
   * @throws IOException
   */
  public MemorizingTrustManager(Options options)
      throws KeyStoreException, NoSuchAlgorithmException,
      CertificateException, FileNotFoundException, IOException {
    this.options=options;

    clear(false);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * javax.net.ssl.X509TrustManager#checkClientTrusted(java
   * .security.cert.X509Certificate[], java.lang.String)
   */
  @Override
  synchronized public void checkClientTrusted(X509Certificate[] chain,
                                              String authType)
                                                              throws CertificateException {
    try {
      storeTrustManager.checkClientTrusted(chain, authType);
    }
    catch (CertificateException e) {
      try {
        transientTrustManager.checkClientTrusted(chain, authType);
      }
      catch (CertificateException e2) {
        if (options.trustOnFirstUse && !options.store.exists()) {
          try {
            storeCert(chain);
          }
          catch (Exception e3) {
            throw new CertificateMemorizationException(e3);
          }
        }
        else {
          throw new CertificateNotMemorizedException(chain);
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * javax.net.ssl.X509TrustManager#checkServerTrusted(java
   * .security.cert.X509Certificate[], java.lang.String)
   */
  @Override
  synchronized public void checkServerTrusted(X509Certificate[] chain,
                                              String authType)
                                                              throws CertificateException {
    try {
      storeTrustManager.checkServerTrusted(chain, authType);
    }
    catch (CertificateException e) {
      try {
        transientTrustManager.checkServerTrusted(chain, authType);
      }
      catch (CertificateException e2) {
        if (options.trustOnFirstUse && !options.store.exists()) {
          try {
            storeCert(chain);
          }
          catch (Exception e3) {
            throw new CertificateMemorizationException(e3);
          }
        }
        else {
          throw new CertificateNotMemorizedException(chain);
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * javax.net.ssl.X509TrustManager#getAcceptedIssuers()
   */
  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return(new X509Certificate[0]);
  }

  /**
   * Memorizes a certificate, by storing it in the
   * persistent key store.
   * 
   * @param chain
   *          user-approved certificate chain
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   */
  synchronized public void storeCert(X509Certificate[] chain)
                                                             throws KeyStoreException,
                                                             NoSuchAlgorithmException,
                                                             CertificateException,
                                                             IOException {
    for (X509Certificate cert : chain) {
      String alias=cert.getSubjectDN().getName();

      keyStore.setCertificateEntry(alias, cert);
    }

    initTrustManager();

    FileOutputStream fos=new FileOutputStream(options.store);

    keyStore.store(fos, options.storePassword.toCharArray());
    fos.close();
  }

  /**
   * Records a certificate chain in the transient key store,
   * for use while this process is going on, but not saved
   * between processes.
   * 
   * @param chain
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   */
  synchronized public void allowOnce(X509Certificate[] chain)
                                                             throws KeyStoreException,
                                                             NoSuchAlgorithmException {
    for (X509Certificate cert : chain) {
      String alias=cert.getSubjectDN().getName();

      transientKeyStore.setCertificateEntry(alias, cert);
    }

    initTrustManager();
  }

  /**
   * Clears the transient key store, and optionally clears
   * the persistent key store (by deleting its file and
   * re-initializing it).
   * 
   * @param clearPersistent
   *          true to clear both key stores, false to clear
   *          only the transient one
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   */
  synchronized public void clear(boolean clearPersistent)
                                                         throws KeyStoreException,
                                                         NoSuchAlgorithmException,
                                                         CertificateException,
                                                         IOException {
    if (clearPersistent) {
      options.store.delete();
    }

    initTransientStore();
    initPersistentStore();
    initTrustManager();
  }

  private void initTransientStore() throws KeyStoreException,
                                   NoSuchAlgorithmException,
                                   CertificateException, IOException {
    transientKeyStore=KeyStore.getInstance(options.storeType);
    transientKeyStore.load(null, null);
  }

  private void initPersistentStore() throws KeyStoreException,
                                    NoSuchAlgorithmException,
                                    CertificateException,
                                    FileNotFoundException, IOException {
    keyStore=KeyStore.getInstance(options.storeType);

    if (options.store.exists()) {
      keyStore.load(new FileInputStream(options.store),
                    options.storePassword.toCharArray());
    }
    else {
      keyStore.load(null, options.storePassword.toCharArray());
    }
  }

  private void initTrustManager() throws KeyStoreException,
                                 NoSuchAlgorithmException {
    TrustManagerFactory tmf=TrustManagerFactory.getInstance("X509");

    tmf.init(keyStore);

    for (TrustManager t : tmf.getTrustManagers()) {
      if (t instanceof X509TrustManager) {
        storeTrustManager=(X509TrustManager)t;
        break;
      }
    }

    tmf=TrustManagerFactory.getInstance("X509");

    tmf.init(transientKeyStore);

    for (TrustManager t : tmf.getTrustManagers()) {
      if (t instanceof X509TrustManager) {
        transientTrustManager=(X509TrustManager)t;
        break;
      }
    }
  }

  /**
   * Configuration options for certificate memorization.
   * This class has a builder-style API, so you can
   * configure an instance via a chained set of method
   * calls.
   */
  public static class Options {
    File workingDir=null;
    File store=null;
    String storePassword;
    String storeType=KeyStore.getDefaultType();
    boolean trustOnFirstUse=false;

    /**
     * Constructor. Note that the Context is not held by the
     * Options instance, and so any handy Context should be
     * fine.
     * 
     * @param ctxt
     *          a Context
     * @param storeRelPath
     *          a relative path within internal storage to a
     *          working directory for the
     *          MemorizingTrustManager (parent directories
     *          will be created for you as needed)
     * @param storePassword
     *          the password under which to store these
     *          certificates
     */
    public Options(Context ctxt, String storeRelPath,
                   String storePassword) {
      workingDir=new File(ctxt.getFilesDir(), storeRelPath);
      workingDir.mkdirs();
      store=new File(workingDir, "memorized.bks");

      this.storePassword=storePassword;
    }

    /**
     * Call this to enable "trust on first use" logic. The
     * first SSL certificate that is seen by the trust
     * manager will automatically be accepted and saved. For
     * cases where this trust manager will only be used for
     * one site, this eliminates the need to prompt the user
     * to validate the certificate. It *does* assume that
     * the first SSL certificate is valid, though if it is
     * not (e.g., the user was a victim of a MITM attack at
     * that point), future work should result in seeing the
     * real certificate and (hopefully) triggering work to
     * realize that something is wrong.
     * 
     * @return the options object for chained method calls
     */
    public Options trustOnFirstUse() {
      trustOnFirstUse=true;

      return(this);
    }

    /**
     * Call this to enable "trust on first use" logic. The
     * first SSL certificate that is seen by the trust
     * manager will automatically be accepted and saved. For
     * cases where this trust manager will only be used for
     * one site, this eliminates the need to prompt the user
     * to validate the certificate. It *does* assume that
     * the first SSL certificate is valid, though if it is
     * not (e.g., the user was a victim of a MITM attack at
     * that point), future work should result in seeing the
     * real certificate and (hopefully) triggering work to
     * realize that something is wrong.
     * 
     * @param trust
     *          true if should trust on first use, false
     *          otherwise
     * @return the options object for chained method calls
     */
    public Options trustOnFirstUse(boolean trust) {
      trustOnFirstUse=trust;

      return(this);
    }
  }
}
