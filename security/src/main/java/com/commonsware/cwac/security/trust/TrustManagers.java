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

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class TrustManagers {
  public static TrustManager[] useTrustStore(InputStream in,
                                             char[] password,
                                             String format)
                                                           throws GeneralSecurityException,
                                                           IOException,
                                                           NullPointerException {
    if (format == null) {
      format=KeyStore.getDefaultType();
    }

    KeyStore store=KeyStore.getInstance(format);

    try {
      store.load(in, password);
    }
    finally {
      in.close();
    }

    TrustManagerFactory tmf=
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

    tmf.init(store);

    return(tmf.getTrustManagers());
  }

  public static TrustManager[] allowCA(InputStream in, String certType)
                                                                       throws CertificateException,
                                                                       IOException,
                                                                       NoSuchAlgorithmException,
                                                                       KeyStoreException {
    Certificate caCert;
    CertificateFactory cf=CertificateFactory.getInstance(certType);

    try {
      caCert=cf.generateCertificate(in);
    }
    finally {
      in.close();
    }

    KeyStore store=KeyStore.getInstance(KeyStore.getDefaultType());

    store.load(null, null);
    store.setCertificateEntry("ca", caCert);

    TrustManagerFactory tmf=
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

    tmf.init(store);

    return(tmf.getTrustManagers());
  }
}
