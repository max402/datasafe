package de.adorsys.datasafe.metainfo.version.api.version;

import de.adorsys.datasafe.types.api.actions.VersionStrategy;

/**
 * Versioning strategy - i.e. read only latest.
 * @param <V> Versioning class.
 */
public interface WithVersionStrategy<V extends VersionStrategy> {

    V getStrategy();
}
