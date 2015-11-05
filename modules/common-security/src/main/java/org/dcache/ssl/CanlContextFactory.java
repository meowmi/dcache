/* dCache - http://www.dcache.org/
 *
 * Copyright (C) 2015 Deutsches Elektronen-Synchrotron
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dcache.ssl;

import com.google.common.base.Throwables;
import eu.emi.security.authn.x509.CrlCheckingMode;
import eu.emi.security.authn.x509.NamespaceCheckingMode;
import eu.emi.security.authn.x509.OCSPCheckingMode;
import eu.emi.security.authn.x509.OCSPParametes;
import eu.emi.security.authn.x509.ProxySupport;
import eu.emi.security.authn.x509.RevocationParameters;
import eu.emi.security.authn.x509.ValidationErrorCategory;
import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.helpers.ssl.SSLTrustManager;
import eu.emi.security.authn.x509.impl.OpensslCertChainValidator;
import eu.emi.security.authn.x509.impl.PEMCredential;
import eu.emi.security.authn.x509.impl.ValidatorParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static eu.emi.security.authn.x509.ValidationErrorCategory.*;
import static eu.emi.security.authn.x509.ValidationErrorCategory.CRL;
import static eu.emi.security.authn.x509.ValidationErrorCategory.OCSP;
import static org.dcache.util.Callables.memoizeFromFiles;
import static org.dcache.util.Callables.memoizeWithExpiration;

/**
 * SslContextFactory based on the CANL library. Uses the builder pattern to
 * create immutable instances.
 */
public class CanlContextFactory implements SslContextFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CanlContextFactory.class);

    private static final EnumSet<ValidationErrorCategory> VALIDATION_ERRORS_TO_LOG =
            EnumSet.of(NAMESPACE, X509_BASIC, X509_CHAIN, NAME_CONSTRAINT, CRL, OCSP);

    private final SecureRandom secureRandom = new SecureRandom();
    private final TrustManager[] trustManagers;

    protected CanlContextFactory(TrustManager... trustManagers)
    {
        this.trustManagers = trustManagers;
    }

    public static CanlContextFactory createDefault()
    {
        return new Builder().build();
    }

    public static Builder custom()
    {
        return new Builder();
    }

    public TrustManager[] getTrustManagers()
    {
        return trustManagers;
    }

    @Override
    public SSLContext getContext(X509Credential credential)
            throws GeneralSecurityException
    {
        KeyManager[] keyManagers = { credential.getKeyManager() };
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers, trustManagers, secureRandom);
        return context;
    }

    public static class Builder
    {
        private Path certificateAuthorityPath = FileSystems.getDefault().getPath("/etc/grid-security/certificates");
        private NamespaceCheckingMode namespaceMode = NamespaceCheckingMode.EUGRIDPMA_GLOBUS;
        private CrlCheckingMode crlCheckingMode = CrlCheckingMode.IF_VALID;
        private OCSPCheckingMode ocspCheckingMode = OCSPCheckingMode.IF_AVAILABLE;
        private long certificateAuthorityUpdateInterval = 600000;
        private boolean lazyMode = true;
        private Path keyPath = FileSystems.getDefault().getPath("/etc/grid-security/hostkey.pem");
        private Path certificatePath = FileSystems.getDefault().getPath("/etc/grid-security/hostcert.pem");
        private long credentialUpdateInterval = 1;
        private TimeUnit credentialUpdateIntervalUnit = TimeUnit.MINUTES;
        private Supplier<AutoCloseable> contextSupplier = () -> () -> {};

        private Builder()
        {
        }

        public Builder withCertificateAuthorityPath(Path certificateAuthorityPath)
        {
            this.certificateAuthorityPath = certificateAuthorityPath;
            return this;
        }

        public Builder withCertificateAuthorityPath(String certificateAuthorityPath)
        {
            return withCertificateAuthorityPath(FileSystems.getDefault().getPath(certificateAuthorityPath));
        }

        public Builder withCertificateAuthorityUpdateInterval(long interval)
        {
            this.certificateAuthorityUpdateInterval = interval;
            return this;
        }

        public Builder withCertificateAuthorityUpdateInterval(long interval, TimeUnit unit)
        {
            this.certificateAuthorityUpdateInterval = unit.toMillis(interval);
            return this;
        }

        public Builder withCrlCheckingMode(CrlCheckingMode crlCheckingMode)
        {
            this.crlCheckingMode = crlCheckingMode;
            return this;
        }

        public Builder withOcspCheckingMode(OCSPCheckingMode ocspCheckingMode)
        {
            this.ocspCheckingMode = ocspCheckingMode;
            return this;
        }

        public Builder withNamespaceMode(NamespaceCheckingMode namespaceMode)
        {
            this.namespaceMode = namespaceMode;
            return this;
        }

        public Builder withLazy(boolean lazyMode)
        {
            this.lazyMode = lazyMode;
            return this;
        }

        public Builder withKeyPath(Path keyPath)
        {
            this.keyPath = keyPath;
            return this;
        }

        public Builder withCertificatePath(Path certificatePath)
        {
            this.certificatePath = certificatePath;
            return this;
        }

        public Builder withCredentialUpdateInterval(long duration, TimeUnit unit)
        {
            this.credentialUpdateInterval = duration;
            this.credentialUpdateIntervalUnit = unit;
            return this;
        }

        public Builder withLoggingContext(Supplier<AutoCloseable> contextSupplier)
        {
            this.contextSupplier = contextSupplier;
            return this;
        }

        public CanlContextFactory build()
        {
            OCSPParametes ocspParameters = new OCSPParametes(ocspCheckingMode);
            ValidatorParams validatorParams =
                    new ValidatorParams(new RevocationParameters(crlCheckingMode, ocspParameters),
                                        ProxySupport.ALLOW);
            X509CertChainValidator v =
                    new OpensslCertChainValidator(certificateAuthorityPath.toString(), true, namespaceMode,
                                                  certificateAuthorityUpdateInterval,
                                                  validatorParams, lazyMode);
            v.addUpdateListener((location, type, level, cause) -> {
                try (AutoCloseable context = contextSupplier.get()) {
                    switch (level) {
                    case ERROR:
                        LOGGER.error("Error loading {} from {}: ", type, location, cause);
                        break;
                    case WARNING:
                        LOGGER.warn("Problem loading {} from {}: ", type, location, cause);
                        break;
                    case NOTIFICATION:
                        LOGGER.info("Reloaded {} from {}: ", type, location);
                        break;
                    }
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
            });
            v.addValidationListener(error -> {
                if (VALIDATION_ERRORS_TO_LOG.contains(error.getErrorCategory())) {
                    X509Certificate[] chain = error.getChain();
                    String subject = (chain != null && chain.length > 0) ? chain[0].getSubjectX500Principal().getName() : "";
                    LOGGER.warn("The peer's certificate with DN {} was rejected: {}", subject, error);
                }
                return false;
            });
            return new CanlContextFactory(new SSLTrustManager(v));
        }

        public Callable<SSLContext> buildWithCaching()
        {
            CanlContextFactory factory = build();
            Callable<SSLContext> newContext =
                    () -> factory.getContext(new PEMCredential(keyPath.toString(), certificatePath.toString(), null));
            return  memoizeWithExpiration(memoizeFromFiles(newContext, keyPath, certificatePath),
                                          credentialUpdateInterval, credentialUpdateIntervalUnit);
        }
    }
}