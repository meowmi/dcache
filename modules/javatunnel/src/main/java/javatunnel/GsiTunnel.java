package javatunnel;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import org.globus.gsi.CredentialException;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GSSConstants;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.gsi.gssapi.auth.AuthorizationException;
import org.globus.gsi.gssapi.jaas.GlobusPrincipal;
import org.gridforum.jgss.ExtendedGSSContext;
import org.gridforum.jgss.ExtendedGSSManager;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.italiangrid.voms.VOMSValidators;
import org.italiangrid.voms.ac.VOMSACValidator;
import org.italiangrid.voms.store.VOMSTrustStore;
import org.italiangrid.voms.store.VOMSTrustStores;
import org.italiangrid.voms.util.CertificateValidatorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import org.dcache.auth.FQAN;
import org.dcache.auth.FQANPrincipal;
import org.dcache.util.Args;
import org.dcache.util.Crypto;

import static java.util.Collections.singletonList;
import static org.dcache.util.Files.checkDirectory;
import static org.dcache.util.Files.checkFile;

class GsiTunnel extends GssTunnel  {

    private static final Logger _log = LoggerFactory.getLogger(GsiTunnel.class);
    private final VOMSTrustStore vomsTrustStore;
    private final X509CertChainValidatorExt certChainValidator;

    private ExtendedGSSContext _e_context;

    private static final String SERVICE_KEY = "service_key";
    private static final String SERVICE_CERT = "service_cert";
    private static final String SERVICE_TRUSTED_CERTS = "service_trusted_certs";
    private static final String SERVICE_VOMS_DIR = "service_voms_dir";
    private static final String CIPHER_FLAGS = "ciphers";

    private final Args _arguments;
    private Subject _subject = new Subject();

    public GsiTunnel(String args)
            throws GSSException, IOException
    {
        _arguments = new Args(args);

        X509Credential serviceCredential;
        String service_key = _arguments.getOption(SERVICE_KEY);
        String service_cert = _arguments.getOption(SERVICE_CERT);
        String caDir = _arguments.getOption(SERVICE_TRUSTED_CERTS);
        String vomsDir = _arguments.getOption(SERVICE_VOMS_DIR);
        vomsTrustStore = VOMSTrustStores.newTrustStore(singletonList(vomsDir));
        certChainValidator = new CertificateValidatorBuilder().trustAnchorsDir(caDir).build();

        /* Unfortunately, we can't rely on GlobusCredential to provide
         * meaningful error messages so we catch some obvious problems
         * early.
         */
        checkFile(service_key);
        checkFile(service_cert);
        checkDirectory(caDir);

        try {
            serviceCredential = new X509Credential(service_cert, service_key);
        } catch (CredentialException e) {
            throw new GSSException(GSSException.NO_CRED, 0, e.getMessage());
        } catch(IOException ioe) {
            throw new GSSException(GSSException.NO_CRED, 0,
                                   "could not load host globus credentials "+ioe.toString());
        }

        GSSCredential cred = new GlobusGSSCredentialImpl(serviceCredential, GSSCredential.ACCEPT_ONLY);
        GSSManager manager = ExtendedGSSManager.getInstance();
        _e_context = (ExtendedGSSContext) manager.createContext(cred);
        _e_context.setOption(GSSConstants.GSS_MODE, GSIConstants.MODE_GSI);
        _e_context.setBannedCiphers(
                Crypto.getBannedCipherSuitesFromConfigurationValue(_arguments.getOption(CIPHER_FLAGS)));
        _context = _e_context;
        // do not use channel binding with GSIGSS
        super.useChannelBinding(false);
    }

    @Override
    public boolean verify(InputStream in, OutputStream out, Object addon) {
        try {
            if (super.verify(in, out, addon)) {
                X509Certificate[] chain = (X509Certificate[]) _e_context.inquireByOid(GSSConstants.X509_CERT_CHAIN);
                _subject.getPublicCredentials().add(chain);
                _subject.getPrincipals().add(new GlobusPrincipal(
                                       _e_context.getSrcName().toString()));
                scanExtendedAttributes(_e_context);
            }
        } catch (GSSException e) {
            _log.error("Failed to verify: {}", e.toString());
        }

        return _context.isEstablished();
    }


    @Override
    public Convertable makeCopy() throws IOException {
        try {
            return new GsiTunnel(_arguments.toString());
        } catch (GSSException e) {
            throw new IOException(e);
        }
    }

    private void scanExtendedAttributes(ExtendedGSSContext gssContext) {

        try {
            X509Certificate[] chain = (X509Certificate[]) gssContext.inquireByOid(GSSConstants.X509_CERT_CHAIN);

            VOMSACValidator validator = VOMSValidators.newValidator(vomsTrustStore, certChainValidator);
            validator.validate(chain).stream().flatMap(a -> a.getFQANs().stream()).map(FQAN::new).forEachOrdered(
                    new Consumer<FQAN>()
                    {
                        boolean primary = true;

                        @Override
                        public void accept(FQAN fqan)
                        {
                            _subject.getPrincipals().add(new FQANPrincipal(fqan, primary));
                            primary = false;
                        }
                    });
        } catch (GSSException e) {
            _log.error("Could not extract certificate chain from context {}", e.getMessage());
        }
    }

    @Override
    public Subject getSubject() {
        return _subject;
    }
}