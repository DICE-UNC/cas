/*
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.cas.util;

import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.crypto.CipherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

/**
 * A {@link CipherExecutor} implementation that is based on algorithms
 * provided by the default platform's JCE. By default AES encryption is
 * used which requires both the secret key and the IV length to be of size 16.
 * @author Misagh Moayyed
 * @since 4.2
 */
@Component("shiroCipherExecutor")
public class ShiroCipherExecutor extends AbstractCipherExecutor<byte[], byte[]> {
    private static final String UTF8_ENCODING = "UTF-8";

    /** Secret key IV algorithm. Default is <code>AES</code>. */
    private String secretKeyAlgorithm = "AES";

    private final String encryptionSecretKey;

    /**
     * Instantiates a new cryptic ticket cipher executor.
     *
     * @param encryptionSecretKey the encryption secret key
     * @param signingSecretKey the signing key
     */
    @Autowired
    public ShiroCipherExecutor(@Value("${ticket.encryption.secretkey:N0$ecr3T}")
                               final String encryptionSecretKey,
                               @Value("${ticket.signing.secretkey:N0$ecr3T}")
                               final String signingSecretKey) {
        super(signingSecretKey);
        this.encryptionSecretKey = encryptionSecretKey;
    }

    @Autowired
    public void setSecretKeyAlgorithm(@Value("${ticket.secretkey.alg:AES}")
                                          final String secretKeyAlgorithm) {
        this.secretKeyAlgorithm = secretKeyAlgorithm;
    }

    @Override
    public byte[] encode(final byte[] value) {
        try {
            final Key key = new SecretKeySpec(this.encryptionSecretKey.getBytes(),
                    this.secretKeyAlgorithm);
            final CipherService cipher = new AesCipherService();
            final byte[] result = cipher.encrypt(value, key.getEncoded()).getBytes();
            return sign(result);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decode(final byte[] value) {
        try {
            final byte[] verifiedValue = verifySignature(value);
            final Key key = new SecretKeySpec(this.encryptionSecretKey.getBytes(UTF8_ENCODING),
                    this.secretKeyAlgorithm);
            final CipherService cipher = new AesCipherService();
            final byte[] result = cipher.decrypt(verifiedValue, key.getEncoded()).getBytes();
            return result;
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


}
