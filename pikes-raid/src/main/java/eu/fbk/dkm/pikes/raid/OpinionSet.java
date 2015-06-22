package eu.fbk.dkm.pikes.raid;

import com.google.common.collect.ForwardingSet;
import ixa.kaflib.Opinion;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by alessio on 02/04/15.
 */

public class OpinionSet extends ForwardingSet<OpinionSet.OpinionEntry> {

	public class OpinionEntry {
		private Opinion opinion;
		private Integer size;

		public OpinionEntry(Opinion opinion) {
			this.opinion = opinion;
			this.size = opinion.getOpinionExpression().getTerms().size();
		}

		public Opinion getOpinion() {
			return opinion;
		}

		public Integer getSize() {
			return size;
		}

		@Override
		public String toString() {
			return "OpinionEntry{" +
					"opinion=" + opinion +
					", size=" + size +
					'}';
		}
	}

	private class OpinionEntryComparator implements Comparator<OpinionEntry> {

		boolean desc = false;

		public OpinionEntryComparator(boolean desc) {
			this.desc = desc;
		}

		@Override
		public int compare(OpinionEntry o1, OpinionEntry o2) {
			int result;
			if (desc) {
				result = o2.getSize() - o1.getSize();
			}
			else {
				result = o1.getSize() - o2.getSize();
			}
			if (result == 0) {
				result = o1.getOpinion().getId().compareTo(o2.getOpinion().getId());
			}

			return result;
		}

	}

	SortedSet<OpinionEntry> support;

	public OpinionSet() {
		this(false);
	}

	@Override
	protected Set<OpinionEntry> delegate() {
		return support;
	}

	public OpinionSet(boolean desc) {
		support = new TreeSet<>(new OpinionEntryComparator(desc));
	}

	public void add(Opinion opinion) {
		OpinionEntry entry;
		try {
			entry = new OpinionEntry(opinion);
		} catch (Exception e) {
			return;
		}
		support.add(entry);
	}

	@Override
	public String toString() {
		return support.toString();
	}
}
