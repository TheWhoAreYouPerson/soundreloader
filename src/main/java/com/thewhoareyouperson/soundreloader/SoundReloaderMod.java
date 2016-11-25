package com.thewhoareyouperson.soundreloader;

import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

@Mod(modid = SoundReloaderMod.MODID, version = SoundReloaderMod.VERSION)
public class SoundReloaderMod {
	public static final String MODID = "soundreloader";
	public static final String VERSION = "1.0";
	public static final Logger Logger = LogManager.getLogger(SoundReloaderMod.MODID);
	
	protected static final boolean devEnv = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
	
	protected static KeyBinding soundBinding;
	
	@EventHandler
	public void preinit(FMLPreInitializationEvent event) {
		soundBinding = new KeyBinding("Reload Default Sound", Keyboard.KEY_P, "key.categories.misc");
		ClientRegistry.registerKeyBinding(soundBinding);
		MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
	}
	
	public static class KeyInputHandler {
		private static SoundManager cachedManager = null;
		
		@SubscribeEvent
	    public void onKeyInput(InputEvent.KeyInputEvent event) {
	        if(SoundReloaderMod.soundBinding.isPressed()){
	        	SoundReloaderMod.Logger.info("Reloading sound system...");
	        	if(cachedManager == null){
		        	SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
		        	SoundManager mngr = null;
		        	Exception ex = null;
		        	try {
						for(Field fd : SoundHandler.class.getDeclaredFields())
							Logger.info("SoundHandler Field: '" + fd.getName() + "' of type '" + fd.getType() + "'");
						Field fi = SoundHandler.class.getDeclaredField(devEnv ? "sndManager" : "field_147694_f");// mcpbot says field_148622_c but MC (code above) says field_147694_f
						fi.setAccessible(true);
						mngr = (SoundManager) fi.get(handler);
					} catch (NoSuchFieldException e) {
						ex = e;
					} catch (SecurityException e) {
						ex = e;
					} catch (IllegalArgumentException e) {
						ex = e;
					} catch (IllegalAccessException e) {
						ex = e;
					} finally {
						if(ex != null)
							ex.printStackTrace();
					}
		        	cachedManager = mngr;
	        	}
	        	
	        	
	        	if(cachedManager != null){
	        		cachedManager.reloadSoundSystem();
	        		SoundReloaderMod.Logger.info("Reloaded sound system");
	        	} else {
	        		SoundReloaderMod.Logger.warn("Unable to get sound manager");
	        	}
	        }
	    }
	}
}
