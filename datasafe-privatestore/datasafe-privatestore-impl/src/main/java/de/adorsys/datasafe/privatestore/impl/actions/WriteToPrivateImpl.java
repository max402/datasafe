package de.adorsys.datasafe.privatestore.impl.actions;

import de.adorsys.datasafe.encrypiton.api.document.EncryptedDocumentWriteService;
import de.adorsys.datasafe.privatestore.api.actions.EncryptedResourceResolver;
import de.adorsys.datasafe.privatestore.api.actions.WriteToPrivate;
import de.adorsys.datasafe.directory.api.profile.keys.PrivateKeyService;
import de.adorsys.datasafe.encrypiton.api.types.UserIDAuth;
import de.adorsys.datasafe.types.api.actions.WriteRequest;
import de.adorsys.datasafe.encrypiton.api.types.keystore.SecretKeyIDWithKey;
import de.adorsys.datasafe.types.api.resource.PrivateResource;

import javax.inject.Inject;
import java.io.OutputStream;

public class WriteToPrivateImpl implements WriteToPrivate {

    private final PrivateKeyService privateKeyService;
    private final EncryptedResourceResolver resolver;
    private final EncryptedDocumentWriteService writer;

    @Inject
    public WriteToPrivateImpl(PrivateKeyService privateKeyService, EncryptedResourceResolver resolver,
                              EncryptedDocumentWriteService writer) {
        this.privateKeyService = privateKeyService;
        this.resolver = resolver;
        this.writer = writer;
    }

    @Override
    public OutputStream write(WriteRequest<UserIDAuth, PrivateResource> request) {
        SecretKeyIDWithKey keySpec = privateKeyService.documentEncryptionSecretKey(request.getOwner());

        return writer.write(
                resolver.encryptAndResolvePath(request.getOwner(), request.getLocation()),
                keySpec
        );
    }
}
