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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Class for building TrustManager instances for use with
 * HttpsURLConnection, OkHTTP, and kin.
 * 
 * This class has a builder-style fluent interface. Create
 * an instance, and you can call various methods on it,
 * chained one after the next, as most methods return the
 * builder itself.
 * 
 * The end of the chained method calls should be build() (to
 * create a single TrustManager representing what you want)
 * or buildArray() (a convenience method to wrap that single
 * TrustManager in a TrustManager[], which many APIs
 * require).
 */
@Deprecated
public class TrustManagerBuilder {
  private static final String X509="X.509";
  private static final String BKS="BKS";

  private CompositeTrustManager mgr=CompositeTrustManager.matchAll();
  private Context ctxt=null;
  private MemorizingTrustManager memo=null;

  /**
   * Empty constructor. Use this only if you plan on
   * avoiding methods that take an asset path or a raw
   * resource ID as parameters. If you want to use those,
   * use the one-parameter constructor that takes a Context
   * as input.
   */
  public TrustManagerBuilder() {
    this(null);
  }

  /**
   * Typical constructor for TrustManagerBuilder, allowing
   * full use of the builder API. The Context you choose
   * should be appropriately scoped for the lifetime of the
   * TrustManagerBuilder. However, the Context is not part
   * of the generated TrustManager. Hence, usually the
   * Context that is the component using the
   * TrustManagerBuilder (e.g., the Service) will be a fine
   * Context to use here.
   * 
   * @param ctxt
   *          Context to use for asset and raw resource
   *          access
   */
  public TrustManagerBuilder(Context ctxt) {
    this.ctxt=ctxt;
  }

  /**
   * @return the TrustManager representing the particular
   *         rules you want to apply
   */
  public TrustManager build() {
    return(mgr);
  }

  /**
   * @return the TrustManager from build(), wrapped into a
   *         one-element array, for convenience
   */
  public TrustManager[] buildArray() {
    return(new TrustManager[] { build() });
  }

  /**
   * Any subsequent configuration of this builder, until the
   * next and() call (or build()/buildArray()), will be
   * logically OR'd with whatever came previously. For
   * example, if you need to support two possible
   * self-signed certificates, use
   * selfSigned(...).or().selfSigned(...) to accept either
   * one.
   * 
   * @return the builder for chained calls
   */
  public TrustManagerBuilder or() {
    if (mgr.isMatchAll()) {
      if (mgr.size() < 2) {
        mgr.setMatchAll(false);
      }
      else {
        mgr=CompositeTrustManager.matchAny(mgr);
      }
    }

    return(this);
  }

  /**
   * Any subsequent configuration of this builder, until the
   * next or() call (or build()/buildArray()), will be
   * logically AND'd with whatever came previously. Note
   * that this is the default state or the builder, so you
   * only need an and() to reverse a previous or().
   * 
   * @return the builder for chained calls
   */
  public TrustManagerBuilder and() {
    if (!mgr.isMatchAll()) {
      if (mgr.size() < 2) {
        mgr.setMatchAll(true);
      }
      else {
        mgr=CompositeTrustManager.matchAll(mgr);
      }
    }

    return(this);
  }

  /**
   * Tells the builder to add the default (system)
   * TrustManagers to the roster of ones to consider. For
   * example, to support normal certificates plus a
   * self-signed certificate, use
   * useDefault().or().selfSigned(...).
   * 
   * @return the builder for chained calls
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   */
  public TrustManagerBuilder useDefault()
                                         throws NoSuchAlgorithmException,
                                         KeyStoreException {
    TrustManagerFactory tmf=
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

    tmf.init((KeyStore)null);
    addAll(tmf.getTrustManagers());

    return(this);
  }

  /**
   * Rejects all certificates. At most, this is useful in
   * testing. Use in a production app will cause us to
   * question your sanity.
   * 
   * @return the builder for chained calls
   */
  public TrustManagerBuilder denyAll() {
    mgr.add(new DenyAllTrustManager());

    return(this);
  }

  /**
   * Allow a specific certificate authority (CA), using a
   * certificate file supplied by that CA. Used if the SSL
   * certificate you wish to accept is not signed by a root
   * CA that is not going to be honored by all Android
   * device (e.g., due to OS version).
   * 
   * The certificate file is assumed to be in X.509 format.
   * To use a different supported Certificate format, use
   * the two-parameter version of allowCA().
   * 
   * @param caFile
   *          certificate file on local filesystem
   * @return the builder for chained calls
   * @throws CertificateException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws IOException
   */
  public TrustManagerBuilder allowCA(File caFile)
                                                 throws CertificateException,
                                                 NoSuchAlgorithmException,
                                                 KeyStoreException,
                                                 IOException {
    return(allowCA(caFile, X509));
  }

  /**
   * Allow a specific certificate authority (CA), using a
   * certificate file supplied by that CA. Used if the SSL
   * certificate you wish to accept is not signed by a root
   * CA that is not going to be honored by all Android
   * device (e.g., due to OS version).
   * 
   * @param caFile
   *          certificate file on local file system
   * @param certType
   *          certificate format
   * @return the builder for chained calls
   * @throws CertificateException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws IOException
   */
  public TrustManagerBuilder allowCA(File caFile, String certType)
                                                                  throws CertificateException,
                                                                  NoSuchAlgorithmException,
                                                                  KeyStoreException,
                                                                  IOException {
    InputStream in=new BufferedInputStream(new FileInputStream(caFile));

    addAll(TrustManagers.allowCA(in, certType));

    return(this);
  }

  /**
   * Allow a specific certificate authority (CA), using a
   * certificate file supplied by that CA. Used if the SSL
   * certificate you wish to accept is not signed by a root
   * CA that is not going to be honored by all Android
   * device (e.g., due to OS version).
   * 
   * The certificate file is assumed to be in X.509 format.
   * To use a different supported Certificate format, use
   * the two-parameter version of allowCA().
   * 
   * @param rawResourceId
   *          raw resource ID for the certificate
   * @return the builder for chained calls
   * @throws CertificateException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws IOException
   */
  public TrustManagerBuilder allowCA(int rawResourceId)
                                                       throws CertificateException,
                                                       NoSuchAlgorithmException,
                                                       KeyStoreException,
                                                       IOException {
    return(allowCA(rawResourceId, X509));
  }

  /**
   * Allow a specific certificate authority (CA), using a
   * certificate file supplied by that CA. Used if the SSL
   * certificate you wish to accept is not signed by a root
   * CA that is not going to be honored by all Android
   * device (e.g., due to OS version).
   * 
   * @param rawResourceId
   *          raw resource ID for the certificate
   * @param certType
   *          certificate format
   * @return the builder for chained calls
   * @throws CertificateException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws IOException
   */
  public TrustManagerBuilder allowCA(int rawResourceId, String certType)
                                                                        throws CertificateException,
                                                                        NoSuchAlgorithmException,
                                                                        KeyStoreException,
                                                                        IOException {
    checkContext();

    InputStream in=ctxt.getResources().openRawResource(rawResourceId);

    addAll(TrustManagers.allowCA(in, certType));

    return(this);
  }

  /**
   * Allow a specific certificate authority (CA), using a
   * certificate file supplied by that CA. Used if the SSL
   * certificate you wish to accept is not signed by a root
   * CA that is not going to be honored by all Android
   * device (e.g., due to OS version).
   * 
   * The certificate file is assumed to be in X.509 format.
   * To use a different supported Certificate format, use
   * the two-parameter version of allowCA().
   * 
   * @param assetPath
   *          path within assets/ of your project where the
   *          certificate file resides
   * @return the builder for chained calls
   * @throws CertificateException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws IOException
   */
  public TrustManagerBuilder allowCA(String assetPath)
                                                      throws CertificateException,
                                                      NoSuchAlgorithmException,
                                                      KeyStoreException,
                                                      IOException {
    return(allowCA(assetPath, X509));
  }

  /**
   * Allow a specific certificate authority (CA), using a
   * certificate file supplied by that CA. Used if the SSL
   * certificate you wish to accept is not signed by a root
   * CA that is not going to be honored by all Android
   * device (e.g., due to OS version).
   * 
   * @param assetPath
   *          path within assets/ of your project where the
   *          certificate file resides
   * @param certType
   *          certificate format
   * @return the builder for chained calls
   * @throws CertificateException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws IOException
   */
  public TrustManagerBuilder allowCA(String assetPath, String certType)
                                                                       throws CertificateException,
                                                                       NoSuchAlgorithmException,
                                                                       KeyStoreException,
                                                                       IOException {
    checkContext();

    InputStream in=ctxt.getAssets().open(assetPath);

    addAll(TrustManagers.allowCA(in, certType));

    return(this);
  }

  /**
   * Support a specific self-signed certificate. The
   * password is a char[] to allow you to wipe out that
   * password (e.g., set all element to nulls) after use, to
   * get rid of it from memory as soon as possible. That, of
   * course, is only relevant if the password is retrievable
   * dynamically and in the form of a char[] (e.g., you read
   * it yourself from a file). For cases where this level of
   * security is unnecessary (e.g., the password is
   * hard-coded), just use toCharArray() on a String to get
   * a char[] to use.
   * 
   * This method assumes that the keystore is in BKS format.
   * If you are using some other format supported by
   * KeyStore, use the three-parameter version of this
   * method().
   * 
   * @param store
   *          path to keystore file on local file system
   * @param password
   *          password to use to access keystore file
   * @return the builder for chained calls
   * @throws NullPointerException
   * @throws GeneralSecurityException
   * @throws IOException
   */
  public TrustManagerBuilder selfSigned(File store, char[] password)
                                                                    throws NullPointerException,
                                                                    GeneralSecurityException,
                                                                    IOException {
    return(selfSigned(store, password, BKS));
  }

  /**
   * Support a specific self-signed certificate. The
   * password is a char[] to allow you to wipe out that
   * password (e.g., set all element to nulls) after use, to
   * get rid of it from memory as soon as possible. That, of
   * course, is only relevant if the password is retrievable
   * dynamically and in the form of a char[] (e.g., you read
   * it yourself from a file). For cases where this level of
   * security is unnecessary (e.g., the password is
   * hard-coded), just use toCharArray() on a String to get
   * a char[] to use.
   * 
   * @param store
   *          path to keystore file on local file system
   * @param password
   *          password to use to access keystore file
   * @param format
   *          format of keystore file
   * @return the builder for chained calls
   * @throws NullPointerException
   * @throws GeneralSecurityException
   * @throws IOException
   */
  public TrustManagerBuilder selfSigned(File store, char[] password,
                                        String format)
                                                      throws NullPointerException,
                                                      GeneralSecurityException,
                                                      IOException {
    InputStream in=new BufferedInputStream(new FileInputStream(store));

    addAll(TrustManagers.useTrustStore(in, password, format));

    return(this);
  }

  /**
   * Support a specific self-signed certificate. The
   * password is a char[] to allow you to wipe out that
   * password (e.g., set all element to nulls) after use, to
   * get rid of it from memory as soon as possible. That, of
   * course, is only relevant if the password is retrievable
   * dynamically and in the form of a char[] (e.g., you read
   * it yourself from a file). For cases where this level of
   * security is unnecessary (e.g., the password is
   * hard-coded), just use toCharArray() on a String to get
   * a char[] to use.
   * 
   * This method assumes that the keystore is in BKS format.
   * If you are using some other format supported by
   * KeyStore, use the three-parameter version of this
   * method().
   * 
   * @param rawResourceId
   *          raw resource ID referencing this keystore
   * @param password
   *          password to use to access keystore file
   * @return the builder for chained calls
   * @throws NullPointerException
   * @throws GeneralSecurityException
   * @throws IOException
   */
  public TrustManagerBuilder selfSigned(int rawResourceId,
                                        char[] password)
                                                        throws NullPointerException,
                                                        GeneralSecurityException,
                                                        IOException {
    return(selfSigned(rawResourceId, password, BKS));
  }

  /**
   * Support a specific self-signed certificate. The
   * password is a char[] to allow you to wipe out that
   * password (e.g., set all element to nulls) after use, to
   * get rid of it from memory as soon as possible. That, of
   * course, is only relevant if the password is retrievable
   * dynamically and in the form of a char[] (e.g., you read
   * it yourself from a file). For cases where this level of
   * security is unnecessary (e.g., the password is
   * hard-coded), just use toCharArray() on a String to get
   * a char[] to use.
   * 
   * @param rawResourceId
   *          raw resource ID referencing this keystore
   * @param password
   *          password to use to access keystore file
   * @param format
   *          format for the keystore
   * @return the builder for chained calls
   * @throws NullPointerException
   * @throws GeneralSecurityException
   * @throws IOException
   */
  public TrustManagerBuilder selfSigned(int rawResourceId,
                                        char[] password, String format)
                                                                       throws NullPointerException,
                                                                       GeneralSecurityException,
                                                                       IOException {
    checkContext();

    InputStream in=ctxt.getResources().openRawResource(rawResourceId);

    addAll(TrustManagers.useTrustStore(in, password, format));

    return(this);
  }

  /**
   * Support a specific self-signed certificate. The
   * password is a char[] to allow you to wipe out that
   * password (e.g., set all element to nulls) after use, to
   * get rid of it from memory as soon as possible. That, of
   * course, is only relevant if the password is retrievable
   * dynamically and in the form of a char[] (e.g., you read
   * it yourself from a file). For cases where this level of
   * security is unnecessary (e.g., the password is
   * hard-coded), just use toCharArray() on a String to get
   * a char[] to use.
   * 
   * This method assumes that the keystore is in BKS format.
   * If you are using some other format supported by
   * KeyStore, use the three-parameter version of this
   * method().
   * 
   * @param assetPath
   *          relative path within assets/ where the
   *          keystore is located
   * @param password
   *          password to use to access keystore file
   * @return the builder for chained calls
   * @throws IOException
   * @throws NullPointerException
   * @throws GeneralSecurityException
   */
  public TrustManagerBuilder selfSigned(String assetPath,
                                        char[] password)
                                                        throws IOException,
                                                        NullPointerException,
                                                        GeneralSecurityException {
    return(selfSigned(assetPath, password, BKS));
  }

  /**
   * Support a specific self-signed certificate. The
   * password is a char[] to allow you to wipe out that
   * password (e.g., set all element to nulls) after use, to
   * get rid of it from memory as soon as possible. That, of
   * course, is only relevant if the password is retrievable
   * dynamically and in the form of a char[] (e.g., you read
   * it yourself from a file). For cases where this level of
   * security is unnecessary (e.g., the password is
   * hard-coded), just use toCharArray() on a String to get
   * a char[] to use.
   * 
   * @param assetPath
   *          relative path within assets/ where the
   *          keystore is located
   * @param password
   *          password to use to access keystore file
   * @param format
   *          keystore format
   * @return the builder for chained calls
   * @throws IOException
   * @throws NullPointerException
   * @throws GeneralSecurityException
   */
  public TrustManagerBuilder selfSigned(String assetPath,
                                        char[] password, String format)
                                                                       throws IOException,
                                                                       NullPointerException,
                                                                       GeneralSecurityException {
    checkContext();

    InputStream in=ctxt.getAssets().open(assetPath);

    addAll(TrustManagers.useTrustStore(in, password, format));

    return(this);
  }

  /**
   * Enables certificate memorization for this builder. All
   * SSL certificates need to be approved by the user before
   * they will be accepted by the TrustManager for an actual
   * HTTPS operation.
   * 
   * @param options
   *          a MemorizingTrustManager.Options instance
   *          configuring the memorization behavior
   * @return the builder for chained calls
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws FileNotFoundException
   * @throws IOException
   */
  public TrustManagerBuilder memorize(MemorizingTrustManager.Options options)
                                                                             throws KeyStoreException,
                                                                             NoSuchAlgorithmException,
                                                                             CertificateException,
                                                                             FileNotFoundException,
                                                                             IOException {
    if (memo != null) {
      throw new IllegalStateException(
                                      "Cannot add a 2nd MemorizingTrustManager");
    }

    memo=new MemorizingTrustManager(options);
    mgr.add(memo);

    return(this);
  }

  /**
   * Use this to add an arbitrary TrustManager[] array to
   * the mix. Only the X509TrustManager instances in the
   * array will be used. This is also used, under the
   * covers, by most of the other builder methods, to add
   * configured trust managers.
   * 
   * @param mgrs
   *          the TrustManager instances to add
   * @return the builder for chained calls
   */
  public TrustManagerBuilder addAll(TrustManager[] mgrs) {
    for (TrustManager tm : mgrs) {
      if (tm instanceof X509TrustManager) {
        mgr.add((X509TrustManager)tm);
      }
    }

    return(this);
  }

  /**
   * If you catch an SSLHandshakeException when performing
   * HTTPS I/O, and its getCause() is a
   * CertificateNotMemorizedException, then you know that
   * you configured certificate memorization using
   * memorize(), and the SSL certificate for your request
   * was not recognized.
   * 
   * If the user agrees that your app should use the SSL
   * certificate forever (or until you clear it), call
   * memorizeCert(), supplying the certificate chain you get
   * by calling getCertificateChain() on the
   * CertificateNotMemorizedException. Note that this will
   * perform disk I/O and therefore should be done on a
   * background thread.
   * 
   * Note that this method is not part of the builder set of
   * methods to configure a TrustManagerBuilder. Instead, it
   * is used at runtime to handle memorization events.
   * 
   * @param chain
   *          user-approved certificate chain
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   */
  public void memorizeCert(X509Certificate[] chain)
                                                   throws KeyStoreException,
                                                   NoSuchAlgorithmException,
                                                   CertificateException,
                                                   IOException {
    memo.storeCert(chain);
  }

  /**
   * If you catch an SSLHandshakeException when performing
   * HTTPS I/O, and its getCause() is a
   * CertificateNotMemorizedException, then you know that
   * you configured certificate memorization using
   * memorize(), and the SSL certificate for your request
   * was not recognized.
   * 
   * If the user agrees that your app should use the SSL
   * certificate for the lifetime of this process only, but
   * not retain it beyond that, call allowCertOnce(),
   * supplying the certificate chain you get by calling
   * getCertificateChain() on the
   * CertificateNotMemorizedException. Once your process is
   * terminated, this cached certificate is lost, and you
   * will get a CertificateNotMemorizedException again later
   * on.
   * 
   * Note that this method is not part of the builder set of
   * methods to configure a TrustManagerBuilder. Instead, it
   * is used at runtime to handle memorization events.
   * 
   * @param chain
   *          user-approved certificate chain
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   */
  public void allowCertOnce(X509Certificate[] chain)
                                                    throws KeyStoreException,
                                                    NoSuchAlgorithmException {
    memo.allowOnce(chain);
  }

  /**
   * If you saved certificates using memorizeCert() or
   * allowCertOnce(), you can get rid of them using this
   * method.
   * 
   * @param clearPersistent
   *          true if you want to clear all certificates
   *          (allow-once and memorized), false if you want
   *          to clear only allow-once certificates
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   */
  public void clearMemorizedCerts(boolean clearPersistent)
                                                          throws KeyStoreException,
                                                          NoSuchAlgorithmException,
                                                          CertificateException,
                                                          IOException {
    memo.clear(clearPersistent);
  }

  private void checkContext() {
    if (ctxt == null) {
      throw new IllegalArgumentException(
                                         "Must use constructor supplying a Context");
    }
  }
}
