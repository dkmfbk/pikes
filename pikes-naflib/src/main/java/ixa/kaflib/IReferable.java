package ixa.kaflib;


public abstract class IReferable implements Comparable<IReferable>, TLinkReferable
{
	@Override
	public int compareTo(IReferable o) {
		return this.getId().compareTo(o.getId());
	}
}
