package com.thewhoareyouperson.soundreloader;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.ParameterizedMessage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.launchwrapper.Launch;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.versioning.ComparableVersion;
import net.minecraftforge.fml.common.versioning.InvalidVersionSpecificationException;
import net.minecraftforge.fml.common.versioning.VersionRange;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(name = SoundReloaderMod.NAME, modid = SoundReloaderMod.MODID, version = SoundReloaderMod.VERSION)
public class SoundReloaderMod {
	public static final String NAME = "Sound Reloader";
	public static final String MODID = "soundreloader";
	public static final String VERSION = "1.1";
	public static final Logger Logger = LogManager.getLogger(SoundReloaderMod.MODID);
	public static final String ERROR_STRING = SoundReloaderMod.NAME + " cannot access the minecraft version, and subsequently, the sound system. This mod will be inactive.";
	public static final List<String> SUPPORTED_VERSIONS = Arrays.asList("1.7.10", "1.8.9", "1.10.2", "1.11.2", "1.12.2");
	
	private static final String DEFAULT_MAPPING = "field_147694_f";
	private static boolean modInactive = false;
	private static String cachedMCVersionString;
	private static ComparableVersion cachedMCVersionObj;
	
	
	protected static final boolean debug = System.getProperty("minecraft." + MODID + ".debug") != null;
	protected static final boolean devEnv = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
	
	protected static KeyBinding soundBinding;
	
	@EventHandler
	public void preinit(FMLPreInitializationEvent event) {
		ComparableVersion mcVersion = getMcVersion();
		if(!modInactive) {
			Logger.info("Minecraft version: '" + mcVersion + "'");
			if(!SUPPORTED_VERSIONS.contains(mcVersion.toString())) {
				Logger.warn("This mod is using an unsupported version of Minecraft. It may still work, but it is recommended to try to update to a version that supports it.");
			}
			
			Logger.info("Registering Keybindings");
			if(devEnv)
				Logger.info("Running in Development (Deobfuscated) Environment");
			
			soundBinding = new KeyBinding("Reload Default Sound Device", Keyboard.KEY_P, "key.categories.misc");
			ClientRegistry.registerKeyBinding(soundBinding);
			
			if(mcVersion.compareTo(new ComparableVersion("1.7.10")) <= 0 ){
				if(debug) Logger.info("Using legacy (1.7.10 and lower) event bus access.");
				FMLCommonHandler.instance().bus().register(new KeyInputHandler());
			} else {
				if(debug) Logger.info("Using current(?) event bus access.");
				MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
			}
		}
	}
	
	public static class KeyInputHandler {
		private static SoundManager cachedManager = null;
		
		@SubscribeEvent
	    public void onKeyInput(InputEvent.KeyInputEvent event) {
	        if(!modInactive && SoundReloaderMod.soundBinding.isPressed()){
	        	SoundReloaderMod.Logger.info("Reloading sound system...");
	        	if(cachedManager == null){
		        	SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
		        	SoundManager mngr = null;
		        	Exception ex = null;
		        	try {
		        		if(debug)
							for(Field fd : SoundHandler.class.getDeclaredFields())
								Logger.info("SoundHandler Field: '" + fd.getName() + "' of type '" + fd.getType() + "'");
		        		Field fi = SoundHandler.class.getDeclaredField(devEnv ? "sndManager" : DEFAULT_MAPPING);// mcpbot says field_148622_c but MC (code above) says field_147694_f
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
	        		SoundReloaderMod.Logger.warn("Unable to get sound manager, shutting down mod.");
					modInactive = true;
	        	}
	        }
	    }
	}
	
	protected static ComparableVersion getMcVersion() {
		if(cachedMCVersionString == null){
			//Get the MC version in a way that doesn't make it a compile time constant (Theoretically allow multiple MC Versions with one jar)
			Class mcfClass = MinecraftForge.class;
			String versionFieldName = "MC_VERSION";
			try {
				Field fldMcVersion = mcfClass.getField(versionFieldName);
				if(fldMcVersion != null)
					cachedMCVersionString = (String) fldMcVersion.get(null);
			} catch (RuntimeException e) {
				Logger.error(new ParameterizedMessage("Cannot get {0} from {1} for some reason!", new Object[]{versionFieldName, mcfClass.getCanonicalName()}, e));
			} catch (NoSuchFieldException e) {
				Logger.error(new ParameterizedMessage("Cannot find {0} from {1}!", new Object[]{versionFieldName, mcfClass.getCanonicalName()}, e));
			} catch (IllegalAccessException e) {
				Logger.error(new ParameterizedMessage("Cannot access {0} from {1}!", new Object[]{versionFieldName, mcfClass.getCanonicalName()}, e));
			} finally {
				if(cachedMCVersionString == null) {
					Logger.warn(ERROR_STRING);
					modInactive = true;
					return null;
				}
			}
		}
		return (cachedMCVersionObj != null) ? cachedMCVersionObj : (cachedMCVersionObj = new ComparableVersion(cachedMCVersionString));
	}
}
