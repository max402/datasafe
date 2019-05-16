package de.adorsys.datasafe.business.api.inbox.actions;

import de.adorsys.datasafe.business.api.encryption.document.EncryptedDocumentReadService;
import de.adorsys.datasafe.business.api.resource.ResourceResolver;
import de.adorsys.datasafe.business.api.types.UserIDAuth;
import de.adorsys.datasafe.business.api.types.action.ReadRequest;
import de.adorsys.datasafe.business.api.types.resource.AbsoluteLocation;
import de.adorsys.datasafe.business.api.types.resource.PrivateResource;

import javax.inject.Inject;
import java.io.InputStream;

public class ReadFromInboxImpl implements ReadFromInbox {

    private final ResourceResolver resolver;
    private final EncryptedDocumentReadService reader;

    @Inject
    public ReadFromInboxImpl(ResourceResolver resolver, EncryptedDocumentReadService reader) {
        this.resolver = resolver;
        this.reader = reader;
    }

    @Override
    public InputStream read(ReadRequest<UserIDAuth, PrivateResource> request) {
        return reader.read(ReadRequest.<UserIDAuth, AbsoluteLocation<PrivateResource>>builder()
                .location(resolver.resolveRelativeToPrivateInbox(request.getOwner(), request.getLocation()))
                .owner(request.getOwner())
                .build());
    }
}