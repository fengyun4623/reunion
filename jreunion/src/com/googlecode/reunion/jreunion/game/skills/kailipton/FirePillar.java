package com.googlecode.reunion.jreunion.game.skills.kailipton;

import java.util.List;
import java.util.Vector;

import com.googlecode.reunion.jreunion.game.Castable;
import com.googlecode.reunion.jreunion.game.KailiptonPlayer;
import com.googlecode.reunion.jreunion.game.LivingObject;
import com.googlecode.reunion.jreunion.game.Mob;
import com.googlecode.reunion.jreunion.game.Player;
import com.googlecode.reunion.jreunion.game.Skill;
import com.googlecode.reunion.jreunion.game.items.equipment.StaffWeapon;
import com.googlecode.reunion.jreunion.game.items.equipment.Weapon;
import com.googlecode.reunion.jreunion.game.skills.Modifier;
import com.googlecode.reunion.jreunion.game.skills.Modifier.ModifierType;
import com.googlecode.reunion.jreunion.game.skills.Modifier.ValueType;
import com.googlecode.reunion.jreunion.server.SkillManager;
import com.googlecode.reunion.jreunion.server.PacketFactory.Type;

public class FirePillar extends Tier2 implements Castable, Modifier {

	public FirePillar(SkillManager skillManager,int id) {
		super(skillManager,id);
	}
	
	public int getMaxLevel() {
		return 25;
	}
	
	public ValueType getValueType() {
		return Modifier.ValueType.FIRE;
	}

	public int getLevelRequirement(int level) {
		return 37+level;
	}
	
	public float getDamageModifier(){
		/* level 1 = 6 (magic damage)
		 * level 2 = 18
		 * level 3 = 21
		 * ...
		 * level 25 = 150
		 */
		
		return (float)144/(getMaxLevel()-1);
		
	}
	
	public float getDamageModifier(Player player){
		
		float modifier = 0;
		int level = player.getSkillLevel(this);
		
		if(level>0){
			modifier += (6+((level-1)*getDamageModifier()));			
			}	
		
		return modifier;
	}
	
	@Override
	public boolean cast(LivingObject caster, LivingObject... targets) {
		if(caster instanceof KailiptonPlayer){
			int currentMana = ((KailiptonPlayer) caster).getMana();
			// mana spent: level 1 = 22 ... level 25 = 66 
			int manaSpent = (int)(22 + ((((KailiptonPlayer) caster).getSkillLevel(this)-1) * ((float)44/(getMaxLevel()-1))));
			
			if((currentMana - manaSpent)  < 0){
				((Player)(caster)).getClient().sendPacket(Type.SAY, "Not enought mana to use the skill.");
				return false;
			}  else {
				((KailiptonPlayer) caster).setMana(currentMana - manaSpent);
			}
			
			Player player = (Player)caster;
			Weapon weapon = player.getEquipment().getMainHand();
			float baseDamage = player.getBaseDamage();
			double weaponMagicBoost = 1;
			if(weapon instanceof StaffWeapon){
				weaponMagicBoost += ((double)weapon.getMagicDmg())/100; // % of magic dmg boost
			}
			float fireDamage = getDamageModifier(player);
			float fireMasteryDamage = 1;
			
			// calculate damage from skills FireBall and FireMastery
			for(Skill skill: ((Player)caster).getSkills().keySet()){
				if (Modifier.class.isInstance(skill)){
					Modifier modifier = (Modifier)skill; 
					if(modifier.getAffectedSkills().contains(this)){
						if(modifier.getValueType() == getValueType()){	
							switch(modifier.getModifierType()){	
								case MULTIPLICATIVE: // FireMastery
									fireMasteryDamage *= modifier.getModifier(caster);
									break;
								case ADDITIVE: // FireBall
									fireDamage += (modifier.getModifier(caster)*0.6);
									break;
							}
						}
					}						
				}
			}
			
			float magicDamage = (float)((baseDamage + fireDamage) * fireMasteryDamage * weaponMagicBoost);
			
			//Todo: this skill can target up to 4 targets
			//(Main target 100% damage, other targets 70% damage)
			synchronized(targets){
				int newHp = targets[0].getHp() - (int) (magicDamage);				
				if (newHp <= 0) {
					if(targets[0] instanceof Mob)
						((Mob)targets[0]).kill((KailiptonPlayer)caster);
				} else {
					targets[0].setHp(newHp);
				}
				return true;
			}
			
		}		
		return false;
	}
	
	public boolean getCondition(LivingObject owner){
		// not needed is the class
		return true;
	}

	@Override
	public ModifierType getModifierType() {

		return Modifier.ModifierType.ADDITIVE;
	}
	
	private int [] affectedSkillIds = {26};
	private List<Skill>  affectedSkills = null ;
	
	@Override
	public List<Skill> getAffectedSkills() {
		synchronized(affectedSkillIds){
			if (affectedSkills==null){
				affectedSkills = new Vector<Skill>();
				for(int skillId:affectedSkillIds){					
					SkillManager skillManager = getSkillManager();
					affectedSkills.add(skillManager.getSkill(skillId));					
				}
			}		
		}		
		return affectedSkills;
	}

	@Override
	public float getModifier(LivingObject livingObject) {
		return getDamageModifier((Player)livingObject);
	}

}