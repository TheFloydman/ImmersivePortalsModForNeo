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
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import qouteall.imm_ptl.core.McHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AxisArgumentType implements ArgumentType<Direction.Axis> {
    
    public static final AxisArgumentType instance = new AxisArgumentType();
    
    public static final DynamicCommandExceptionType exceptionType =
        new DynamicCommandExceptionType(object ->
            Component.literal("Invalid Axis " + object)
        );
    
    public static <S> Direction.Axis getAxis(CommandContext<S> context, String argName) {
        return context.getArgument(argName, Direction.Axis.class);
    }
    
    @Override
    public Direction.Axis parse(StringReader reader) throws CommandSyntaxException {
        String s = reader.readUnquotedString();
        return switch (s) {
            case "x", "X" -> Direction.Axis.X;
            case "y", "Y" -> Direction.Axis.Y;
            case "z", "Z" -> Direction.Axis.Z;
            default -> throw exceptionType.createWithContext(reader, s);
        };
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
            Arrays.stream(Direction.Axis.values())
                .map(Enum::name)
                .collect(Collectors.toList()),
            builder
        );
    }
    
    @Override
    public Collection<String> getExamples() {
        return Arrays.stream(Direction.Axis.values())
            .map(Enum::toString).collect(Collectors.toList());
    }
    
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(RegisterEvent.class, registerEvent -> {
            registerEvent.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.key(),
                    McHelper.newResourceLocation("imm_ptl:axis"),
                    () -> ArgumentTypeInfos.registerByClass(AxisArgumentType.class, SingletonArgumentInfo.contextFree(() -> instance)));
        });
    }
}
