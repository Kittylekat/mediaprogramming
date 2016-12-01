package de.sb.broker.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.xml.bind.annotation.XmlElement;

import org.glassfish.hk2.api.AnnotationLiteral;
import org.glassfish.jersey.message.filtering.EntityFiltering;

import de.sb.java.validation.Inequal;
import de.sb.java.validation.Inequal.Operator;

@Entity
@Table(schema = "broker", name = "Bid", uniqueConstraints = @UniqueConstraint(columnNames = { "bidderReference", "auctionReference" }))
@PrimaryKeyJoinColumn(name = "bidIdentity")
@Inequal(leftAccessPath = "price", rightAccessPath = { "auction", "askingPrice" } , operator = Inequal.Operator.GREATER_EQUAL )
@Inequal(leftAccessPath = { "auction", "seller" , "identity" }, rightAccessPath = {  "bidder" , "identity" } , operator = Inequal.Operator.NOT_EQUAL)

public class Bid extends BaseEntity  {

	@XmlElement
    @Min(value = 1)
    @Column(nullable= false, updatable= true)
	private long price;
	
	@ManyToOne
	@JoinColumn(name ="auctionReference")
	private final Auction auction;
	
	@JoinColumn(name ="bidderReference")
	@ManyToOne
	private final Person bidder;

	public Bid(Auction auction,Person bidder){
		
		this.price = auction == null ? 1 : auction.getAskingPrice();
		this.auction = auction;		
		this.bidder = bidder;
	}
	
	protected Bid(){
		this(null,null);
	}
	
	public long getPrice(){
		return this.price;
	}
	
	public void setPrice(long price) {
		this.price =  price;
	}
	
	public Auction getAuction() {
		return auction;
	}
	
	public long getAuctionReference() {
		return this.auction == null ? 0 : this.auction.getIdentity();
	}
	
	public Person getBidder() {
		return bidder;
	}
	
	public long getBidderReference() {
		return this.bidder == null ? 0 : this.bidder.getIdentity();
	}	
	
	/**
	 * Filter annotation for associated bidders marshaled as entities.
	 */
	@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	@EntityFiltering
	@SuppressWarnings("all")
	static public @interface XmlBidderAsEntityFilter {
		static final class Literal extends AnnotationLiteral<XmlBidderAsEntityFilter> implements XmlBidderAsEntityFilter {}
	}

	/**
	 * Filter annotation for associated bidders marshaled as references.
	 */
	@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	@EntityFiltering
	@SuppressWarnings("all")
	static public @interface XmlBidderAsReferenceFilter {
		static final class Literal extends AnnotationLiteral<XmlBidderAsReferenceFilter> implements XmlBidderAsReferenceFilter {};
	}

	/**
	 * Filter annotation for associated auctions marshaled as entities.
	 */
	@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	@EntityFiltering
	@SuppressWarnings("all")
	static public @interface XmlAuctionAsEntityFilter {
		static final class Literal extends AnnotationLiteral<XmlAuctionAsEntityFilter> implements XmlAuctionAsEntityFilter {}
	}

	/**
	 * Filter annotation for associated auctions marshaled as references.
	 */
	@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	@EntityFiltering
	@SuppressWarnings("all")
	static public @interface XmlAuctionAsReferenceFilter {
		static final class Literal extends AnnotationLiteral<XmlAuctionAsReferenceFilter> implements XmlAuctionAsReferenceFilter {}
	}
	
}

