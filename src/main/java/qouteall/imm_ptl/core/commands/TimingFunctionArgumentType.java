package qouteall.imm_ptl.core.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import qouteall.imm_ptl.core.McHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TimingFunctionArgumentType implements ArgumentType<TimingFunction> {
    
    public static final TimingFunctionArgumentType instance = new TimingFunctionArgumentType();
    
    public static final DynamicCommandExceptionType exceptionType =
        new DynamicCommandExceptionType(object ->
            Component.literal("Invalid Timing Function "+object)
        );
    
    public static TimingFunction get(CommandContext<?> context, String argName) {
        return context.getArgument(argName, TimingFunction.class);
    }
    
    @Override
    public TimingFunction parse(StringReader reader) throws CommandSyntaxException {
        String s = reader.readUnquotedString();
        
        // will throw IllegalArgumentException if invalid
        return TimingFunction.valueOf(s);
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
            Arrays.stream(TimingFunction.values())
                .map(Enum::name)
                .collect(Collectors.toList()),
            builder
        );
    }
    
    @Override
    public Collection<String> getExamples() {
        return Arrays.stream(TimingFunction.values())
            .map(Enum::toString).collect(Collectors.toList());
    }
    
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(RegisterEvent.class, registerEvent -> {
            registerEvent.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.key(),
                    McHelper.newResourceLocation("imm_ptl:timing_function_argument_type"),
                    () -> ArgumentTypeInfos.registerByClass(TimingFunctionArgumentType.class, SingletonArgumentInfo.contextFree(() -> instance)));
        });
    }
}
