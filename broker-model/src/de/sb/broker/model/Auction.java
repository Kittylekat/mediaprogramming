package de.sb.broker.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;

import org.glassfish.hk2.api.AnnotationLiteral;
import org.glassfish.jersey.message.filtering.EntityFiltering;

import de.sb.java.validation.Inequal;
import de.sb.java.validation.Inequal.Operator;

@Entity
@Table(schema = "broker", name = "Auction", indexes = @Index(columnList = "closureTimestamp", unique = true))
@PrimaryKeyJoinColumn(name = "auctionIdentity")
@Inequal(leftAccessPath = { "closureTimestamp" }, rightAccessPath = { "creationTimestamp" })

public class Auction extends BaseEntity {

	@XmlElement
	@Column(nullable = false, updatable = true, length = 255)
	@Size(min = 1, max = 255)
	@NotNull
	private String title;
	
	@XmlElement
	@Column(nullable = false, updatable = true)
	@Min(1)
	@NotNull
	private short unitCount;
	
	@XmlElement
	@Column(nullable = false, updatable = true)
	@NotNull
	@Min(0)
	private long askingPrice;
	
	@XmlElement
	@Column(nullable = false, updatable = true)
	@NotNull
	private long closureTimestamp;
	
	@XmlElement
	@Column(nullable = false, updatable = true, length = 8189)
	@Size(min = 1, max = 8189)
	@NotNull
	private String description;
	
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "sellerReference", nullable = false, updatable = false)
	//@NotNull
	private Person seller;
	
	@OneToMany(mappedBy = "auction")
	//@NotNull
	private Set<Bid> bids;

	public Auction(Person seller) {
		//TODO defaults
		this.title = "";
		this.unitCount = 0;
		this.askingPrice = 0;
		this.closureTimestamp = System.currentTimeMillis() + (30*24*60*60*1000);;// +30*24*60*60*1000 oder Duration
		this.description = "";
		this.seller = seller;
		bids = new HashSet<Bid>();
	}
	
	protected Auction(){
		this(null);
	}

	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public short getUnitCount() {
		return unitCount;
	}

	public void setUnitCount(short unitCount) {
		this.unitCount = unitCount;
	}
	
	public long getAskingPrice() {
		return askingPrice;
	}
	
	public void setAskingPrice(long askingPrice) {
		this.askingPrice = askingPrice;
	}

	public long getClosureTimestamp() {
		return closureTimestamp;
	}
	
	public void setClosureTimestamp(long closureTimestamp) {
		this.closureTimestamp = closureTimestamp;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Person getSeller() {
		return seller;
	}
	
	public void setSeller (Person seller){
		this.seller = seller;
	}

	public long getSellerReference() {
		return this.seller == null ? 0 : this.seller.getIdentity();
	}

	public Bid getBid(Person bidder) {
		for (Bid bid : this.bids) {
			if (bid.getBidder().getIdentity() == bidder.getIdentity()) {
				return bid;
			}
		}
		return null;
	}

	public boolean isClosed() {
		return this.closureTimestamp > System.currentTimeMillis();
	}

	public boolean isSealed() {
		return this.isClosed() || !bids.isEmpty();
	}

	/**
	 * Filter annotation for associated sellers marshaled as entities.
	 */
	@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	@EntityFiltering
	@SuppressWarnings("all")
	static public @interface XmlSellerAsEntityFilter {
		static final class Literal extends AnnotationLiteral<XmlSellerAsEntityFilter> implements XmlSellerAsEntityFilter {}
	}

	/**
	 * Filter annotation for associated sellers marshaled as references.
	 */
	@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	@EntityFiltering
	@SuppressWarnings("all")
	static public @interface XmlSellerAsReferenceFilter {
		static final class Literal extends AnnotationLiteral<XmlSellerAsReferenceFilter> implements XmlSellerAsReferenceFilter {}
	}

	/**
	 * Filter annotation for associated bids marshaled as entities.
	 */
	@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	@EntityFiltering
	@SuppressWarnings("all")
	static public @interface XmlBidsAsEntityFilter {
		static final class Literal extends AnnotationLiteral<XmlBidsAsEntityFilter> implements XmlBidsAsEntityFilter {}
	}

}
