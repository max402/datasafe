package de.adorsys.datasafe.privatestore.impl;

import de.adorsys.datasafe.privatestore.api.PrivateSpaceService;
import de.adorsys.datasafe.privatestore.api.actions.ListPrivate;
import de.adorsys.datasafe.privatestore.api.actions.ReadFromPrivate;
import de.adorsys.datasafe.privatestore.api.actions.RemoveFromPrivate;
import de.adorsys.datasafe.privatestore.api.actions.WriteToPrivate;
import lombok.experimental.Delegate;

import javax.inject.Inject;

public class PrivateSpaceServiceImpl implements PrivateSpaceService {

    @Delegate
    private final ListPrivate listPrivate;

    @Delegate
    private final ReadFromPrivate readFromPrivate;

    @Delegate
    private final WriteToPrivate writeToPrivate;

    @Delegate
    private final RemoveFromPrivate removefromPrivate;

    @Inject
    public PrivateSpaceServiceImpl(
            ListPrivate listPrivate,
            ReadFromPrivate readFromPrivate,
            WriteToPrivate writeToPrivate,
            RemoveFromPrivate removeFromPrivate) {
        this.listPrivate = listPrivate;
        this.readFromPrivate = readFromPrivate;
        this.writeToPrivate = writeToPrivate;
        this.removefromPrivate = removeFromPrivate;
    }
}
