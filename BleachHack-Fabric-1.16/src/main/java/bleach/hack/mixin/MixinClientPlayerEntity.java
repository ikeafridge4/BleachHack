/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDrinker420/bleachhack-1.14/).
 * Copyright (c) 2019 Bleach.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package bleach.hack.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;

import bleach.hack.BleachHack;
import bleach.hack.event.events.EventClientMove;
import bleach.hack.event.events.EventSendMovementPackets;
import bleach.hack.module.ModuleManager;
import bleach.hack.module.mods.BetterPortal;
import bleach.hack.module.mods.EntityControl;
import bleach.hack.module.mods.Freecam;
import bleach.hack.module.mods.NoSlow;
import bleach.hack.module.mods.SafeWalk;
import bleach.hack.module.mods.Scaffold;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity extends AbstractClientPlayerEntity {

	@Shadow private float field_3922;

	@Shadow protected MinecraftClient client;

	public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
		super(world, profile);
	}

	@Shadow protected void autoJump(float float_1, float float_2) {}

	@Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
	public void sendMovementPackets(CallbackInfo info) {
		EventSendMovementPackets event = new EventSendMovementPackets();
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			info.cancel();
		}
	}

	@Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"),
			require = 0 /* TODO: proper meteor fix */)
	private boolean tickMovement_isUsingItem(ClientPlayerEntity player) {
		if (ModuleManager.getModule(NoSlow.class).isToggled() && ModuleManager.getModule(NoSlow.class).getSetting(5).asToggle().state) {
			return false;
		}

		return player.isUsingItem();
	}

	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	public void move(MovementType movementType_1, Vec3d vec3d_1, CallbackInfo info) {
		EventClientMove event = new EventClientMove(movementType_1, vec3d_1);
		BleachHack.eventBus.post(event);
		if (event.isCancelled()) {
			info.cancel();
		} else if (!movementType_1.equals(event.type) || !vec3d_1.equals(event.vec3d)) {
			double double_1 = this.getX();
			double double_2 = this.getZ();
			super.move(event.type, event.vec3d);
			this.autoJump((float) (this.getX() - double_1), (float) (this.getZ() - double_2));
			info.cancel();
		}
	}

	@Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
	protected void pushOutOfBlocks(double double_1, double double_2, CallbackInfo ci) {
		if (ModuleManager.getModule(Freecam.class).isToggled()) {
			ci.cancel();
		}
	}

	@Redirect(method = "updateNausea", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;closeHandledScreen()V", ordinal = 0))
	private void updateNausea_closeHandledScreen(ClientPlayerEntity player) {
		if (!ModuleManager.getModule(BetterPortal.class).isToggled()
				|| !ModuleManager.getModule(BetterPortal.class).getSetting(0).asToggle().state) {
			closeHandledScreen();
		}
	}

	@Redirect(method = "updateNausea", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;openScreen(Lnet/minecraft/client/gui/screen/Screen;)V", ordinal = 0))
	private void updateNausea_openScreen(MinecraftClient player, Screen screen_1) {
		if (!ModuleManager.getModule(BetterPortal.class).isToggled()
				|| !ModuleManager.getModule(BetterPortal.class).getSetting(0).asToggle().state) {
			client.openScreen(screen_1);
		}
	}

	@Override
	protected boolean clipAtLedge() {
		return super.clipAtLedge() || ModuleManager.getModule(SafeWalk.class).isToggled()
				|| (ModuleManager.getModule(Scaffold.class).isToggled()
						&& ModuleManager.getModule(Scaffold.class).getSetting(8).asToggle().state);
	}

	@Overwrite
	public float method_3151() {
		return ModuleManager.getModule(EntityControl.class).isToggled()
				&& ModuleManager.getModule(EntityControl.class).getSetting(2).asToggle().state ? 1F : field_3922;
	}
}
