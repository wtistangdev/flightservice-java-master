/*******************************************************************************
* Copyright (c) 2017 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package com.acmeair.securityutils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

public class SecurityUtils {
    
  protected Logger logger =  Logger.getLogger(SecurityUtils.class.getName());
    
  // TODO: Hardcode for now
  private static final String secretKey = "acmeairsecret128";
    
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String SHA_256 = "SHA-256";
  private static final String UTF8 = "UTF-8";

  /**
   * Build hash from data.
   */
  public String buildHash(String data) 
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
    MessageDigest md = MessageDigest.getInstance(SHA_256);
    md.update(data.getBytes(UTF8));
    byte[] digest = md.digest();
    return Base64.getEncoder().encodeToString(digest);
  }
   
  /**
   * Build hmac from all that junk.
   */
  public String buildHmac(String method, String baseUri, String userId, String dateString, 
          String sigBody)
            throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        
    List<String> toHash = new ArrayList<String>();
    toHash.add(method);
    toHash.add(baseUri);
    toHash.add(userId);
    toHash.add(dateString);
    toHash.add(sigBody);
        
    Mac mac = Mac.getInstance(HMAC_ALGORITHM);
    mac.init(new SecretKeySpec(secretKey.getBytes(UTF8), HMAC_ALGORITHM));

    for (String s: toHash) {
      mac.update(s.getBytes(UTF8));
    }
        
    return Base64.getEncoder().encodeToString(mac.doFinal());
  }
    
  /**
   * Verify Body hash.
   */
  public boolean verifyBodyHash(String body, String sigBody) {
        
    if (sigBody.isEmpty()) {
      throw new WebApplicationException("Invalid signature (sigBody)", Status.FORBIDDEN);
    }

    if (body == null || body.length() == 0) {
      throw new WebApplicationException("Invalid signature (body)", Status.FORBIDDEN);
    }

    try {
      String bodyHash = buildHash(body);
      if (!sigBody.equals(bodyHash)) {
        throw new WebApplicationException("Invalid signature (bodyHash)", Status.FORBIDDEN);
      }
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      throw new WebApplicationException("Unable to generate hash", Status.FORBIDDEN);
    }

    return true;
  }
   
  /**
   * Verify signature.
   */
  public boolean verifyFullSignature(String method, 
          String baseUri, 
          String userId, 
          String dateString, 
          String sigBody,
          String signature) {
    try {
      String hmac = buildHmac(method,baseUri,userId,dateString,sigBody);
            
      if (!signature.equals(hmac)) {
        throw new WebApplicationException("Invalid signature (hmacCompare)", Status.FORBIDDEN);
      }
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
      throw new WebApplicationException("Invalid signature", Status.FORBIDDEN);
    }

    return true;
  }
}
