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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Deprecated
public class CertificateNotMemorizedException extends
    CertificateException {
  /**
   * a value courtesy of an Eclipse generator...
   */
  private static final long serialVersionUID=-4093879253540605439L;
  X509Certificate[] chain=null;
  
  public CertificateNotMemorizedException(X509Certificate[] chain) {
    super("Certificate not found in keystore");
    
    this.chain=chain;
  }
  
  public X509Certificate[] getCertificateChain() {
    return(chain);
  }
}
