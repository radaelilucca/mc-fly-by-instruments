package com.radaeli.aeronauticsflybyinstruments.sound;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, AeronauticsFlyByInstruments.MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> BUZZER = SOUNDS.register("buzzer",
            () -> SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            AeronauticsFlyByInstruments.MOD_ID, "buzzer")));

    private ModSounds() { }
}
