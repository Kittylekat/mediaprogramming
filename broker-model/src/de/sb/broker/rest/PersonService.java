package de.sb.broker.rest;

import static de.sb.broker.model.Person.Group.ADMIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

import de.sb.broker.model.*;

@Path("/people")
public class PersonService {
	
	//EntityManagerFactory emfactory = Persistence.createEntityManagerFactory( "broker" );
	//EntityManager em = emfactory.createEntityManager( );
	//static EntityManagerFactory ENTITY_MANAGER_FACTORY;
	//final EntityManager em = ENTITY_MANAGER_FACTORY.createEntityManager();

	//GET /people: Returns the people matching the given criteria, with null or missing parameters identifying omitted criteria.	
	@GET
	@Produces({"application/xml", "application/json"})
	public Person[] getPeople(
			@HeaderParam("Authorization") final String authentication,
			@QueryParam("resultOffset") int resultOffset,
			@QueryParam("resultLength") int resultLength,
			@QueryParam("alias") String alias,
			@QueryParam("firstName") String firstName,
			@QueryParam("familyName") String familyName,
			@QueryParam("email") String email,
			@QueryParam("phone") String phone,
			@QueryParam("street") String street,
			@QueryParam("postcode") String postcode,
			@QueryParam("city") String city,
			@QueryParam("creationtimeLower") Long creationtimeLower,
			@QueryParam("creationtimeUpper") Long creationtimeUpper){
		
		final EntityManager brokerManager = LifeCycleProvider.brokerManager();
		LifeCycleProvider.authenticate(authentication);
		TypedQuery<Long> query = brokerManager.createQuery("select p.identity from Person as p WHERE"
				+ "(:alias is null or p.alias = :alias) and"
				+ "(:firstName is null or p.name.given = :firstName) and"
				+ "(:familyName is null or p.name.family = :familyName) and"
				+ "(:email is null or p.contact.email = :email) and"
				+ "(:phone is null or p.contact.phone = :phone) and"
				+ "(:street is null or p.address.street = :street) and"
				+ "(:postcode is null or p.address.postcode = :postcode) and"
				+ "(:city is null or p.address.city = :city) and"
				+ "(:creationtimeLower is null or p.creationTimestamp >= :creationtimeLower) and"
				+ "(:creationtimeUpper is null or p.creationTimestamp <= :creationtimeUpper)", Long.class);		query.setParameter("alias", alias);
		query.setParameter("firstName", firstName);
		query.setParameter("familyName", familyName);
		query.setParameter("email", email);
		query.setParameter("phone", phone);
		query.setParameter("street", street);
		query.setParameter("postcode", postcode);
		query.setParameter("city", city);
		query.setParameter("creationtimeLower", creationtimeLower);
		query.setParameter("creationtimeUpper", creationtimeUpper);
		
		if (resultOffset > 0) query.setFirstResult(resultOffset);
		if (resultLength > 0) query.setMaxResults(resultLength);
		
		Collection<Person> allPeople = new TreeSet<Person>(Comparator.comparing(Person::getAlias));
		Collection<Long> allPeopleIds = query.getResultList();
		for (long personId : allPeopleIds) {
				final Person person = brokerManager.find(Person.class, personId);
				if (person != null) allPeople.add(person);
		}
		return allPeople.toArray(new Person[0]);
	}
	
	
	//GET /people/{identity}: Returns the person matching the given identity.
	@GET
	@Path("{identity}")
	@Produces({"application/xml", "application/json"})
	public Person getPerson(
			@PathParam("identity") long identity,
			@HeaderParam("Authorization") final String authentication) {
		final EntityManager brokerManager = LifeCycleProvider.brokerManager();
		LifeCycleProvider.authenticate(authentication);
		final Person person = brokerManager.find(Person.class, identity);
		if (person != null){
			return person;
		} else throw new NotFoundException();
	}
	
	//GET /people/{identity}/auctions: Returns all auctions associated with the person matching the given identity (as seller or bidder).
	@GET
	@Path("/{identity}/auctions")
	@Produces({"application/xml", "application/json"})
	public Response getAuctions(
			@PathParam("identity") long identity,
			@QueryParam("ResultOffset") int ResultOffset,
			@QueryParam("ResultLength") int ResultLength,
			@QueryParam("seller") Boolean seller,
			@QueryParam("closed") Boolean closed,
			@HeaderParam("Authorization") final String authentication) {
		
		final EntityManager brokerManager = LifeCycleProvider.brokerManager();
		LifeCycleProvider.authenticate(authentication);
		final Person person = brokerManager.find(Person.class, identity);
		if (person == null) {
			throw new NotFoundException();
		}
		Collection<Auction> allAuctions = new TreeSet<Auction>(Comparator.comparing(Auction::getTitle));
		GenericEntity<?> wrapper = new GenericEntity<Collection<Auction>>(allAuctions) {};
		Annotation[] filterAnnotations = new Annotation[] {};
		
		if (closed != null){
			for(Auction auction : person.getAuctions()){
				if(auction.isClosed() == closed){
					allAuctions.add(auction);
				}
			}
			for (Bid bid : person.getBids()) {
				if (bid.getAuction().isClosed() == closed){
					allAuctions.add(bid.getAuction());
				}
			}
			if(closed){
				filterAnnotations = new Annotation[] { new Auction.XmlSellerAsReferenceFilter.Literal(), new Auction.XmlBidsAsEntityFilter.Literal(), new Bid.XmlBidderAsEntityFilter.Literal(),new Bid.XmlAuctionAsReferenceFilter.Literal()};
			}
		} else{
			allAuctions.addAll(person.getAuctions());
			for (Bid bid : person.getBids()) {
				allAuctions.add(bid.getAuction());
			}
		}
		if(seller != null){
			if(seller){
				for (Iterator<Auction> auctionIterator = allAuctions.iterator(); auctionIterator.hasNext(); ){
					final Auction auction = auctionIterator.next();
					if(auction.getSellerReference() != identity){
						auctionIterator.remove();
					}
				}
			}
			else{
				for (Iterator<Auction> auctionIterator = allAuctions.iterator(); auctionIterator.hasNext(); ){
					final Auction auction = auctionIterator.next();
					if (auction.getSellerReference() == identity){
						auctionIterator.remove();
					}
				}
				if(closed){
					filterAnnotations = new Annotation[] {new Bid.XmlBidderAsEntityFilter.Literal(), new Auction.XmlBidsAsEntityFilter.Literal(), new Bid.XmlAuctionAsReferenceFilter.Literal(),  new Auction.XmlSellerAsEntityFilter.Literal()};
				} else {
					filterAnnotations = new Annotation[] {new Auction.XmlSellerAsEntityFilter.Literal()};
				}
			}
		}
		return Response.ok().entity(wrapper, filterAnnotations).build();
	}
	
	//GET /people/{identity}/bids: Returns all bids for closed auctions associated with the bidder matching the given identity.
	@GET
	@Path("/{identity}/bids")
	@Produces({"application/xml", "application/json"})
	@Bid.XmlBidderAsReferenceFilter
	@Bid.XmlAuctionAsReferenceFilter
	public Bid[] getBids(
		@HeaderParam("Authorization") final String authentication,
		@PathParam("identity") long identity,
		@QueryParam("ResultOffset") int ResultOffset,
		@QueryParam("ResultLength") int ResultLength) {
	
		final EntityManager brokerManager = LifeCycleProvider.brokerManager();
		final Person requester = LifeCycleProvider.authenticate(authentication);
		final Person person = brokerManager.find(Person.class, identity);
		if (person == null) throw new NotFoundException();
		
		Collection<Bid> bids = new TreeSet<Bid>(Comparator.comparing(Bid::getPrice));
		
		if (requester.equals(person)) {
			bids.addAll(person.getBids());
		} else{
			for (Bid bid : person.getBids()) {
				if (bid.getAuction().isClosed()){
					bids.add(bid);
				}
			}
		}
		return bids.toArray(new Bid[0]);
	}

	//GET /people/requester (new): Returns the authenticated requester, which is useful for login operations.
	@GET
	@Path("/requester")
	@Produces({"application/xml", "application/json"})
	public Person getRequester(
			@HeaderParam("Authorization") final String authentication){
		return LifeCycleProvider.authenticate(authentication);
	}
	
	//PUT /people: Requesters that are not part of group ADMIN are both forbidden to alter other people, and to set their own group to ADMIN.
	@PUT
	@Consumes({"application/xml", "application/json"})
	public Long createOrUpdatePerson(
			@Valid @NotNull Person template,
			@HeaderParam("Authorization") final String authentication,
			@HeaderParam("Password") final String password) {
		
		final EntityManager brokerManager = LifeCycleProvider.brokerManager();
		final Person requester = LifeCycleProvider.authenticate(authentication);
		
		final boolean persist = template.getIdentity() == 0;
		final Person person;
		if(persist){
			person = new Person();
		} else if (requester.getGroup() == ADMIN || requester.getIdentity() == template.getIdentity()){
			person = brokerManager.find(Person.class, template.getIdentity());
			if (person == null) throw new NotFoundException();
		} else {
			throw new ForbiddenException();
		}
		
		
		person.setAlias(template.getAlias());
		person.setGroup(template.getGroup());
		person.getName().setFamily(template.getName().getFamily());
		person.getName().setGiven(template.getName().getGiven());
		person.getAddress().setCity(template.getAddress().getCity());
		person.getAddress().setPostCode(template.getAddress().getPostCode());
		person.getAddress().setStreet(template.getAddress().getStreet());
		person.getContact().setEmail(template.getContact().getEmail());
		person.getContact().setPhone(template.getContact().getPhone());
		person.setPasswordHash(password);
		person.setVersion(template.getVersion());
		
		try {
			if (persist) brokerManager.persist(person);	
			else {
				brokerManager.flush();
			}
		} catch (ConstraintViolationException e) {
			throw new ClientErrorException(BAD_REQUEST);
		}

		try {
			brokerManager.getTransaction().commit();
		} catch (RollbackException e) {
			throw new ClientErrorException(CONFLICT);
		}
		finally {
			brokerManager.getTransaction().begin();
		}
		return person.getIdentity();
	}
}

