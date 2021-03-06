package thexfactor117.lsc.events;

import baubles.api.BaublesApi;
import baubles.api.cap.BaublesCapabilities;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import thexfactor117.lsc.LootSlashConquer;
import thexfactor117.lsc.capabilities.cap.CapabilityPlayerInformation;
import thexfactor117.lsc.capabilities.cap.CapabilityPlayerStats;
import thexfactor117.lsc.capabilities.implementation.PlayerInformation;
import thexfactor117.lsc.capabilities.implementation.Stats;
import thexfactor117.lsc.config.Configs;
import thexfactor117.lsc.items.base.ItemBauble;
import thexfactor117.lsc.loot.Attribute;
import thexfactor117.lsc.network.PacketUpdateCoreStats;
import thexfactor117.lsc.network.PacketUpdateStats;
import thexfactor117.lsc.player.PlayerStatUtils;
import thexfactor117.lsc.util.NBTHelper;

/**
 * 
 * @author TheXFactor117
 *
 */
@Mod.EventBusSubscriber
public class EventPlayerTick 
{
	private static int ticks;
	private static int regenTicks;
	
	/* Called ever second to check for slots and update stat bonuses. Might need to be re-worked. */
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent event)
	{
		if (event.phase == Phase.START && !event.player.getEntityWorld().isRemote)
		{
			ticks++;
			regenTicks++;
			
			// TODO: possibly optimize this? 3 packets get sent every second
			if (ticks % Configs.ticksPerStatUpdate == 0)
			{
				PlayerInformation playerInfo = (PlayerInformation) event.player.getCapability(CapabilityPlayerInformation.PLAYER_INFORMATION, null);
				
				if (event.player != null && playerInfo != null)
				{	
					updateStats(event.player, playerInfo, 3);
				}
				
				ticks = 0;
			}
			
			if (regenTicks % 100 == 0)
			{
				Stats statsCap = (Stats) event.player.getCapability(CapabilityPlayerStats.STATS, null);
				
				if (statsCap != null)
				{
					if (statsCap.getMana() < statsCap.getMaxMana())
					{
						statsCap.increaseMana(statsCap.getManaPerSecond());
					}
					
					if (event.player.getHealth() < event.player.getMaxHealth())
					{
						event.player.heal(statsCap.getHealthPerSecond());
					}
					
					LootSlashConquer.network.sendTo(new PacketUpdateStats(statsCap), (EntityPlayerMP) event.player);
				}
				
				regenTicks = 0;
			}
		}
	}
	
	/**
	 * Updates bonus stats.
	 * 
	 * Flag 1 = update armor only
	 * Flag 2 = update baubles only
	 * Flag 3 = update all
	 * 
	 * @param player
	 * @param info
	 * @param flag
	 */
	public static void updateStats(EntityPlayer player, PlayerInformation info, int flag)
	{	
		info.removeBonusStats();
		
		if (flag == 1 || flag == 3)
		{
			for (ItemStack stack : player.inventory.armorInventory)
			{
				if (stack.getItem() instanceof ItemArmor)
				{
					NBTTagCompound nbt = NBTHelper.loadStackNBT(stack);
					
					if (Attribute.STRENGTH.hasAttribute(nbt)) info.setBonusStrengthStat(info.getBonusStrengthStat() + (int) Attribute.STRENGTH.getAmount(nbt));
					if (Attribute.AGILITY.hasAttribute(nbt)) info.setBonusAgilityStat(info.getBonusAgilityStat() + (int) Attribute.AGILITY.getAmount(nbt));
					if (Attribute.DEXTERITY.hasAttribute(nbt)) info.setBonusDexterityStat(info.getBonusDexterityStat() + (int) Attribute.DEXTERITY.getAmount(nbt));
					if (Attribute.INTELLIGENCE.hasAttribute(nbt)) info.setBonusIntelligenceStat(info.getBonusIntelligenceStat() + (int) Attribute.INTELLIGENCE.getAmount(nbt));
					if (Attribute.WISDOM.hasAttribute(nbt)) info.setBonusWisdomStat(info.getBonusWisdomStat() + (int) Attribute.WISDOM.getAmount(nbt));
					if (Attribute.FORTITUDE.hasAttribute(nbt)) info.setBonusFortitudeStat(info.getBonusFortitudeStat() + (int) Attribute.FORTITUDE.getAmount(nbt));
					if (Attribute.ALL_STATS.hasAttribute(nbt))
					{
						info.setBonusStrengthStat(info.getBonusStrengthStat() + (int) Attribute.ALL_STATS.getAmount(nbt));
						info.setBonusAgilityStat(info.getBonusAgilityStat() + (int) Attribute.ALL_STATS.getAmount(nbt));
						info.setBonusDexterityStat(info.getBonusDexterityStat() + (int) Attribute.ALL_STATS.getAmount(nbt));
						info.setBonusIntelligenceStat(info.getBonusIntelligenceStat() + (int) Attribute.ALL_STATS.getAmount(nbt));
						info.setBonusWisdomStat(info.getBonusWisdomStat() + (int) Attribute.ALL_STATS.getAmount(nbt));
						info.setBonusFortitudeStat(info.getBonusFortitudeStat() + (int) Attribute.ALL_STATS.getAmount(nbt));
					}
				}
			}
		}
		
		if (flag == 2 || flag == 3)
		{
			if (player.hasCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null))
			{
				IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
				
				for (int i = 0; i < baubles.getSlots(); i++)
				{
					if (baubles.getStackInSlot(i).getItem() instanceof ItemBauble)
					{
						NBTTagCompound nbt = NBTHelper.loadStackNBT(baubles.getStackInSlot(i));
						
						if (Attribute.STRENGTH.hasAttribute(nbt)) info.setBonusStrengthStat(info.getBonusStrengthStat() + (int) Attribute.STRENGTH.getAmount(nbt));
						if (Attribute.AGILITY.hasAttribute(nbt)) info.setBonusAgilityStat(info.getBonusAgilityStat() + (int) Attribute.AGILITY.getAmount(nbt));
						if (Attribute.DEXTERITY.hasAttribute(nbt)) info.setBonusDexterityStat(info.getBonusDexterityStat() + (int) Attribute.DEXTERITY.getAmount(nbt));
						if (Attribute.INTELLIGENCE.hasAttribute(nbt)) info.setBonusIntelligenceStat(info.getBonusIntelligenceStat() + (int) Attribute.INTELLIGENCE.getAmount(nbt));
						if (Attribute.WISDOM.hasAttribute(nbt)) info.setBonusWisdomStat(info.getBonusWisdomStat() + (int) Attribute.WISDOM.getAmount(nbt));
						if (Attribute.FORTITUDE.hasAttribute(nbt)) info.setBonusFortitudeStat(info.getBonusFortitudeStat() + (int) Attribute.FORTITUDE.getAmount(nbt));
						if (Attribute.ALL_STATS.hasAttribute(nbt))
						{
							info.setBonusStrengthStat(info.getBonusStrengthStat() + (int) Attribute.ALL_STATS.getAmount(nbt));
							info.setBonusAgilityStat(info.getBonusAgilityStat() + (int) Attribute.ALL_STATS.getAmount(nbt));
							info.setBonusDexterityStat(info.getBonusDexterityStat() + (int) Attribute.ALL_STATS.getAmount(nbt));
							info.setBonusIntelligenceStat(info.getBonusIntelligenceStat() + (int) Attribute.ALL_STATS.getAmount(nbt));
							info.setBonusWisdomStat(info.getBonusWisdomStat() + (int) Attribute.ALL_STATS.getAmount(nbt));
							info.setBonusFortitudeStat(info.getBonusFortitudeStat() + (int) Attribute.ALL_STATS.getAmount(nbt));
						}
					}
				}
			}
		}
		
		PlayerStatUtils.updateAttributes(player);
		
		LootSlashConquer.network.sendTo(new PacketUpdateCoreStats(info), (EntityPlayerMP) player);
	}
}
