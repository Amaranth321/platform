package platform.access;

/**
 * These will limit the assignable features. They are not meant to be subsets of another, nor hierarchical
 *
 * @author Aye Maung
 * @since v4.4
 */
public enum FeatureRestriction
{
    NONE,
    NODE_ONLY,
    CLOUD_ONLY,
    SUPERADMIN_ONLY
}
