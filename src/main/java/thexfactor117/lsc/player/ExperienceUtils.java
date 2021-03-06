package thexfactor117.lsc.player;

import java.util.Random;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import thexfactor117.lsc.LootSlashConquer;
import thexfactor117.lsc.capabilities.cap.CapabilityEnemyInfo;
import thexfactor117.lsc.capabilities.implementation.EnemyInfo;
import thexfactor117.lsc.capabilities.implementation.PlayerInformation;
import thexfactor117.lsc.config.Configs;
import thexfactor117.lsc.entities.EntityMonster;
import thexfactor117.lsc.network.PacketUpdatePlayerInformation;

/**
 * 
 * @author TheXFactor117
 *
 */
public class ExperienceUtils 
{
	/** Returns the amount of experience needed to level up given the current level. */
	public static int getLevelUpExperience(int currentLevel) 
	{
		return (int) (Math.pow(currentLevel + 1, Configs.playerCategory.levelUpExpPower) + Configs.playerCategory.levelUpAdditive);
	}
	
	public static void addExperience(EntityPlayer player, PlayerInformation playerInfo, EntityLivingBase enemy)
	{
		int experience = 0;
		
		EnemyInfo enemyInfo = (EnemyInfo) enemy.getCapability(CapabilityEnemyInfo.ENEMY_INFO, null);
		
		// calculates how much experience a monster should drop
		// based on the level, tier, and rarity of the monster
		// ALL vanilla and non-LSC monsters have a rarity of 1.
		if (enemyInfo != null)
		{
			int enemyLevel = enemyInfo.getEnemyLevel();
			int enemyTier = enemyInfo.getEnemyTier();
			int rarity = 1;
			
			if (enemy instanceof EntityMonster)
			{
				rarity = EntityMonster.rarity;
			}
			
			// calculates the different multipliers and multiplies them together to get the total multiplier
			double baseFactor = Configs.monsterLevelTierCategory.experienceBaseFactor;
			double tierMultiplier = (Math.pow(enemyTier, Configs.monsterLevelTierCategory.experienceTierPower) / Configs.monsterLevelTierCategory.experienceTierDivisor + 1) + 0.5;
			double rarityMultiplier = (Math.pow(rarity, Configs.monsterLevelTierCategory.experienceRarityPower) / Configs.monsterLevelTierCategory.experienceRarityDivisor + 1) + 0.5;
			int multiplier = (int) ((tierMultiplier * rarityMultiplier + 1) / Configs.monsterLevelTierCategory.experienceDivisor);
			
			// base experience is 10 for now...
			experience = (int) (Math.pow(baseFactor, enemyLevel + 1) * (Configs.monsterLevelTierCategory.baseExperience + multiplier));
		}
		
		// update experience on client AND server; increase level if need be.
		playerInfo.setPlayerExperience(playerInfo.getPlayerExperience() + experience);
		
		// Minecraft bug with actionbar times - it doesn't actually handle specifying times for the actionbar.
		SPacketTitle packetActionbar = new SPacketTitle(SPacketTitle.Type.ACTIONBAR, new TextComponentString("You killed " + enemy.getName() + " and gained " + experience + " experience!"), -1, -1, -1);
		((EntityPlayerMP) player).connection.sendPacket(packetActionbar);
		
		while (playerInfo.getPlayerExperience() > getLevelUpExperience(playerInfo.getPlayerLevel())) 
		{
			int leftOverExperience = playerInfo.getPlayerExperience() - getLevelUpExperience(playerInfo.getPlayerLevel());
			int skillPoints = 0;
			
			playerInfo.setPlayerLevel(playerInfo.getPlayerLevel() + 1); // increase level
			playerInfo.setPlayerExperience(leftOverExperience);
			
			if (Configs.playerCategory.useTieredSkillPointDistribution)
			{
				// Every level add 1 skill point.
				// Every 5 levels add an additional 2 skill points (total 3)
				// Every 10 levels add an additional 4 skill points (total 5)
				if (playerInfo.getPlayerLevel() % 10 == 0) skillPoints = Configs.playerCategory.skillPointsPer10Levels;
				else if (playerInfo.getPlayerLevel() % 5 == 0) skillPoints = Configs.playerCategory.skillPointsPer5Levels;
				else skillPoints = Configs.playerCategory.skillPointsPerLevel;
			}
			else
			{
				skillPoints = Configs.playerCategory.skillPointsPerLevel;
			}
			
			SPacketTitle packetTitle = new SPacketTitle(SPacketTitle.Type.TITLE, new TextComponentString("Level " + playerInfo.getPlayerLevel()));
			SPacketTitle packetSubtitle = new SPacketTitle(SPacketTitle.Type.SUBTITLE, new TextComponentString("You have leveled up! You've gained " + skillPoints + " Skill Points."));
			((EntityPlayerMP) player).connection.sendPacket(packetTitle);
			((EntityPlayerMP) player).connection.sendPacket(packetSubtitle);
			
			if (Configs.playerCategory.spawnLevelUpParticles)
			{
				spawnLevelUpParticles(player.getEntityWorld(), player, playerInfo.getPlayerLevel()); // bugged
			}
			
			playerInfo.setSkillPoints(playerInfo.getSkillPoints() + skillPoints);
		}
		
		LootSlashConquer.network.sendTo(new PacketUpdatePlayerInformation(playerInfo), (EntityPlayerMP) player); 
	}
	
	private static void spawnLevelUpParticles(World world, EntityPlayer player, int level)
	{
		Random rand = world.rand;
		
		if (level % 10 == 0)
		{
			for (int i = 0; i < 100; i++)
			{
				world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, player.posX, player.posY + 2, player.posZ, rand.nextGaussian() * 0.05D, rand.nextGaussian() * 0.5D, rand.nextGaussian() * 0.05D);
			}
		}
		else if (level % 5 == 0)
		{
			for (int i = 0; i < 100; i++)
			{
				world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, player.posX, player.posY + 2, player.posZ, rand.nextGaussian() * 0.05D, rand.nextGaussian() * 0.5D, rand.nextGaussian() * 0.05D);
			}
		}
		else
		{
			for (int i = 0; i < 100; i++)
			{
				world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, player.posX, player.posY + 2, player.posZ, rand.nextGaussian() * 0.05D, 0, rand.nextGaussian() * 0.05D);
			}
		}
	}
}
