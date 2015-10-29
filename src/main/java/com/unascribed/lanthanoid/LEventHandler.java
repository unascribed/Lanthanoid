package com.unascribed.lanthanoid;

import com.unascribed.lanthanoid.item.ItemRifle;
import com.unascribed.lanthanoid.network.BeamParticleMessage;
import com.unascribed.lanthanoid.network.SetScopeFactorMessage;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityEvent;

public class LEventHandler {
	
	@SubscribeEvent
	public void onConstruct(EntityEvent.EntityConstructing e) {
		e.entity.registerExtendedProperties("lanthanoid", new LanthanoidProperties());
	}
	
	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent e) {
		LanthanoidProperties props = (LanthanoidProperties)e.player.getExtendedProperties("lanthanoid");
		if (props.scopeFactor > 1) {
			ItemStack held = e.player.getHeldItem();
			if (held == null || held.getItem() != LItems.rifle || LItems.rifle.getAttachment(held) != ItemRifle.Attachment.ZOOM) {
				props.scopeFactor = 1;
				if (!e.player.worldObj.isRemote && e.player instanceof EntityPlayerMP) {
					Lanthanoid.inst.network.sendTo(new SetScopeFactorMessage(props.scopeFactor), (EntityPlayerMP)e.player);
				}
			}
		} else if (props.scopeFactor == 0) {
			ItemStack held = e.player.getHeldItem();
			if (held == null || held.getItem() != LItems.rifle || LItems.rifle.getAttachment(held) != ItemRifle.Attachment.NONE) {
				props.scopeFactor = 1;
				if (!e.player.worldObj.isRemote && e.player instanceof EntityPlayerMP) {
					Lanthanoid.inst.network.sendTo(new SetScopeFactorMessage(props.scopeFactor), (EntityPlayerMP)e.player);
				}
			}
		}
		if (props.grabbedEntity != null) {
			ItemStack held = e.player.getHeldItem();
			if (props.grabbedEntity.isDead) {
				props.grabbedEntity = null;
			} else if (held == null || held.getItem() != LItems.rifle || LItems.rifle.getMode(held) != ItemRifle.Mode.TRACTOR) {
				props.grabbedEntity = null;
			} else {
				Entity ent = props.grabbedEntity;
				if (ent instanceof EntityFallingBlock) {
					((EntityFallingBlock)ent).field_145812_b = 2;
				}
				ent.fallDistance = 0;
				Vec3 entPos = Vec3.createVectorHelper(ent.posX, ent.posY, ent.posZ);
				Vec3 look = e.player.getLookVec();
				Vec3 target = Vec3.createVectorHelper(e.player.posX+(look.xCoord*2), e.player.posY+(look.yCoord*2), e.player.posZ+(look.zCoord*2));
				Vec3 dir = entPos.subtract(target);
				ent.setVelocity(dir.xCoord, dir.yCoord+0.2, dir.zCoord);
				if (e.player.worldObj.isRemote && e.player instanceof EntityPlayerMP) {
					EntityPlayerMP player = (EntityPlayerMP)e.player;
					Vec3 start = Vec3.createVectorHelper(player.posX, player.boundingBox.maxY-0.2f, player.posZ);
					if (((LanthanoidProperties)player.getExtendedProperties("lanthanoid")).scopeFactor > 1) {
						start.yCoord -= 0.25;
					} else {
						Vec3 right = player.getLookVec();
						right.rotateAroundY(-90f);
						float rightAdj = 0.25f;
						start = start.addVector(right.xCoord*rightAdj, right.yCoord*rightAdj, right.zCoord*rightAdj);
					}
					Lanthanoid.inst.network.sendToAllAround(new BeamParticleMessage(false, false, start.xCoord, start.yCoord, start.zCoord, ent.posX, ent.posY, ent.posZ, ItemRifle.Mode.TRACTOR.color), new TargetPoint(
							player.worldObj.provider.dimensionId,
							start.xCoord,
							start.yCoord,
							start.zCoord,
							150
							));
				}
			}
		}
	}
	
}
