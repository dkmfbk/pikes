package eu.fbk.dkm.pikes.resources;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Set;

public final class VerbNet {

    public static Set<String> getSuperClasses(final boolean recursive, final String classID) {
        final int index = classID.lastIndexOf('-');
        if (index <= 0 || !Character.isDigit(classID.charAt(index - 1))) {
            return ImmutableSet.of();
        } else {
            final String parent = classID.substring(0, index);
            final Set<String> result = Sets.newHashSet(parent);
            if (recursive) {
                result.addAll(getSuperClasses(true, parent));
            }
            return result;
        }
    }

}
