package com.googlecode.reunion.jreunion.server;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.googlecode.reunion.jcommon.ParsedItem;
import com.googlecode.reunion.jcommon.Parser;
import com.googlecode.reunion.jreunion.events.Event;
import com.googlecode.reunion.jreunion.events.EventBroadcaster;
import com.googlecode.reunion.jreunion.events.EventListener;
import com.googlecode.reunion.jreunion.events.map.MapEvent;
import com.googlecode.reunion.jreunion.game.Merchant;
import com.googlecode.reunion.jreunion.game.Npc;
import com.googlecode.reunion.jreunion.game.Player;
import com.googlecode.reunion.jreunion.game.RoamingItem;
import com.googlecode.reunion.jreunion.game.Spawn;

/**
 * @author Aidamina
 * @license http://reunion.googlecode.com/svn/trunk/license.txt
 */
public class LocalMap extends Map{

	
	private List<Spawn> mobSpawnList = new Vector<Spawn>();

	private List<Npc> npcSpawnList = new Vector<Npc>();

	private Area playerArea = new Area();

	private Area mobArea = new Area();
	
	
	private java.util.List<Session> sessions = new Vector<Session>();	
	
	//<ItemID,ItemContainer>
	public java.util.Map<Integer,RoamingItem> roamingItems = new HashMap<Integer,RoamingItem>();
	
	private Area pvpArea = new Area();

	private Parser playerSpawnReference;


	private Parser mobSpawnReference;

	private Parser npcSpawnReference;



	public LocalMap(int id) {
		super(id);
		
	}

	public void addMobSpawn(Spawn spawn) {
		if (spawn == null) {
			return;
		}
		mobSpawnList.add(spawn);

	}

	public void addNpcSpawn(Npc npc) {
		if (npc == null) {
			return;
		}
		npcSpawnList.add(npc);

	}

	public void createMobSpawns() {

		if (mobSpawnReference == null) {
			return;
		}

		mobSpawnList.clear();
		Iterator<ParsedItem> iter = mobSpawnReference.getItemListIterator();

		while (iter.hasNext()) {

			ParsedItem item = iter.next();

			if (!item.checkMembers(new String[] { "ID", "X", "Y",
					"Radius", "RespawnTime", "Type" })) {
				System.out.println("Error loading a mob spawn on map: "
						+ getId());
				continue;
			}

			Spawn g = new Spawn();

			g.setCenterX(Integer.parseInt(item.getMemberValue("X")));
			g.setCenterY(Integer.parseInt(item.getMemberValue("Y")));
			g.setRadius(Integer.parseInt(item.getMemberValue("Radius")));
			g.setMobType(Integer.parseInt(item.getMemberValue("Type")));
			g.setRespawnTime(Integer.parseInt(item
					.getMemberValue("RespawnTime")));
			g.setMap(this);

			addMobSpawn(g);
			g.spawnMob();
		}
	}

	public void createNpcSpawns() {

		if (npcSpawnReference == null) {
			return;
		}

		npcSpawnList.clear();

		Iterator<ParsedItem> iter = npcSpawnReference.getItemListIterator();

		while (iter.hasNext()) {

			ParsedItem i = iter.next();

			if (!i.checkMembers(new String[] { "ID", "X", "Y",
					"Rotation", "Type" })) {
				System.out.println("Error loading a npc spawn on map: "
						+ getId());
				continue;
			}
			Npc newNpc = Server.getInstance().getWorldModule()
					.getNpcManager()
					.createNpc(Integer.parseInt(i.getMemberValue("Type")));
			newNpc.getPosition().setX(Integer.parseInt(i.getMemberValue("X")));
			newNpc.getPosition().setY(Integer.parseInt(i.getMemberValue("Y")));
			newNpc.getPosition().setRotation(Double.parseDouble(i.getMemberValue("Rotation")));
			newNpc.setSpawnId(Integer.parseInt(i.getMemberValue("ID")));
			newNpc.getPosition().setMap(this);

			if (newNpc instanceof Merchant) {
				newNpc.setSellRate(Integer.parseInt(i
						.getMemberValue("SellRate")));
				newNpc.setBuyRate(Integer.parseInt(i.getMemberValue("BuyRate")));
				newNpc.setShop(i.getMemberValue("Shop"));
				newNpc.loadNpc();
			}
		}
	}

	/**
	 * @return Returns the mapid.
	 */
	
	public Area getMobArea() {
		return mobArea;
	}

	public Area getPlayerArea() {
		return playerArea;
	}

	public Parser getPlayerSpawnReference() {
		return playerSpawnReference;
	}

	public Area getPvpArea() {
		return pvpArea;
	}

	public Spawn getSpawnByMob(int entityID) {
		Iterator<Spawn> mobSpawnIter = mobSpawnListIterator();

		while (mobSpawnIter.hasNext()) {
			Spawn spawn = mobSpawnIter.next();
			if (spawn.getMob().getEntityId() == entityID) {
				return spawn;
			}
		}
		return null;
	}

	public void load() {
		super.load();

		
			
			System.out.println("Loading "+this.getName());
			Server.getInstance().getNetworkModule().register(getAddress());
			playerSpawnReference = new Parser();
			mobSpawnReference = new Parser();
			npcSpawnReference = new Parser();
			loadFromReference(getId());
			createMobSpawns();
			createNpcSpawns();
			System.out.println(getName()+" running on "+getAddress());
			
		
	}
	


	public void loadFromReference(int id) {
		try{
		playerSpawnReference.Parse("data/"+Reference.getInstance().getMapReference()
				.getItemById(id).getMemberValue("PlayerSpawn"));		
		mobSpawnReference.Parse("data/"+Reference.getInstance().getMapReference()
				.getItemById(id).getMemberValue("MobSpawn"));
		npcSpawnReference.Parse("data/"+Reference.getInstance().getMapReference()
				.getItemById(id).getMemberValue("NpcSpawn"));
		
		} catch(Exception e){			
			e.printStackTrace();			
		}

		playerArea.load(Reference.getInstance().getMapReference()
				.getItemById(id).getMemberValue("PlayerArea"));
		mobArea.load(Reference.getInstance().getMapReference()
				.getItemById(id).getMemberValue("MobArea"));
		pvpArea.load(Reference.getInstance().getMapReference()
				.getItemById(id).getMemberValue("PvpArea"));
	}

	public Iterator<Spawn> mobSpawnListIterator() {
		return mobSpawnList.iterator();
	}

	public Iterator<Npc> npcSpawnListIterator() {
		return npcSpawnList.iterator();
	}

	public void workSpawns() {
		Iterator<Spawn> mobSpawnIter = mobSpawnListIterator();

		while (mobSpawnIter.hasNext()) {
			Spawn spawn = mobSpawnIter.next();
			if (spawn == null) {
				continue;
			}

			if (spawn.readyToSpawn()) {
				spawn.spawnMob();

				Iterator<Player> playerIter = Server.getInstance()
						.getWorldModule().getPlayerManager()
						.getPlayerListIterator();

				while (playerIter.hasNext()) {
					Player player = playerIter.next();

					if (player.getPosition().getMap() != spawn.getMob().getPosition().getMap()) {
						continue;
					}

					Client client = player.getClient();

					if (client == null) {
						continue;
					}

					double xcomp = Math.pow(player.getPosition().getX()
							- spawn.getMob().getPosition().getX(), 2);
					double ycomp = Math.pow(player.getPosition().getY()
							- spawn.getMob().getPosition().getY(), 2);
					double distance = Math.sqrt(xcomp + ycomp);

					if (distance < player.getSessionRadius()) {
						player.getSession().enter(spawn.getMob()); //TODO: fix spawn
					}
				}
			}
		}
	}



	@Override
	public void handleEvent(Event event) {
		
		
		if(event instanceof MapEvent){
			LocalMap map = ((MapEvent)event).getMap();
			
			
			
			
		}
		
	}
}