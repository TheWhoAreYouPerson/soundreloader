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
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid = SoundReloaderMod.MODID, version = SoundReloaderMod.VERSION)
public class SoundReloaderMod {
	static {
		String tmpMcVersion;
		{ //Get the MC version in a way that doesn't make it a compile time constant
			Field fldMcVersion;
			try {
				fldMcVersion = MinecraftForge.class.getField("MC_VERSION");
			} catch (Exception e) {
				fldMcVersion = null;
			}
			
			try {
				tmpMcVersion = (String) fldMcVersion.get(null);
			} catch (IllegalArgumentException e) {
				tmpMcVersion = null;
			} catch (IllegalAccessException e) {
				tmpMcVersion = null;
			}
		}
		mcVersion = tmpMcVersion;
	}
	
	public static final String MODID = "soundreloader";
	public static final String VERSION = "1.1";
	public static final Logger Logger = LogManager.getLogger(SoundReloaderMod.MODID);
	
	protected static final String mcVersion;
	protected static final boolean debug = System.getenv("DEBUG") != null && !System.getenv("DEBUG").isEmpty();
	protected static final boolean devEnv = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
	
	protected static KeyBinding soundBinding;
	
	@EventHandler
	public void preinit(FMLPreInitializationEvent event) {
		Logger.info("Registering Keybindings");
		Logger.info("Minecraft version: '" + mcVersion + "'");
		if(devEnv)
			Logger.info("Running in Development (Deobfuscated) Environment");
		
		soundBinding = new KeyBinding("Reload Default Sound", Keyboard.KEY_P, "key.categories.misc");
		ClientRegistry.registerKeyBinding(soundBinding);
		if("1.7.10".equals(mcVersion)){
			FMLCommonHandler.instance().bus().register(new KeyInputHandler());
		} else if("1.8.9".equals(mcVersion) || "1.10.2".equals(mcVersion)) { //Others. Namely, 1.8.9 and 1.10.2
			MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
		} else {
			Logger.warn("Unknown MC Version " + mcVersion + ", not initializing keybind handler.");
		}
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
		        		if(debug)
		        			for(Field fd : SoundHandler.class.getDeclaredFields())
								Logger.info("SoundHandler Field: '" + fd.getName() + "' of type '" + fd.getType() + "'");
		        		Field fi;
		        		if(devEnv){
							fi = SoundHandler.class.getDeclaredField("sndManager");// mcpbot says field_148622_c but MC (code above) says field_147694_f
		        		} else {
		        			fi = SoundHandler.class.getDeclaredField("field_147694_f");// mcpbot says field_148622_c but MC (code above) says field_147694_f
		        		}
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
