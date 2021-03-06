package me.hwproj.mafiagame;

import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import me.hwproj.mafiagame.content.phases.abstractpick.PickState;
import me.hwproj.mafiagame.content.phases.doctor.DoctorPhase;
import me.hwproj.mafiagame.content.phases.mafia.MafiaPhase;
import me.hwproj.mafiagame.content.phases.vote.VotePhase;
import me.hwproj.mafiagame.content.phases.vote.VotePhaseClient;
import me.hwproj.mafiagame.content.phases.vote.VotePhaseGameState;
import me.hwproj.mafiagame.content.phases.vote.VotePhaseServer;
import me.hwproj.mafiagame.gameflow.PlayerSettings;
import me.hwproj.mafiagame.gameflow.Settings;
import me.hwproj.mafiagame.gameplay.Role;
import me.hwproj.mafiagame.content.phases.impltest.TestPhase;
import me.hwproj.mafiagame.content.phases.impltest.TestPhaseClient;
import me.hwproj.mafiagame.content.phases.impltest.TestPhaseGameState;
import me.hwproj.mafiagame.content.phases.impltest.TestPhaseServer;
import me.hwproj.mafiagame.networking.serialization.DeserializationException;
import me.hwproj.mafiagame.networking.serialization.GamePhaseSerializer;
import me.hwproj.mafiagame.networking.serialization.SerializationException;
import me.hwproj.mafiagame.phase.GamePhase;

import static org.junit.jupiter.api.Assertions.*;

public class SerializationTest {

    private ByteArrayOutputStream byteStream;

    private DataOutputStream getOutput() {
        byteStream = new ByteArrayOutputStream();
        return new DataOutputStream(byteStream);
    }

    private DataInputStream getInput() {
        return new DataInputStream(new ByteArrayInputStream(byteStream.toByteArray()));
    }

    @Test
    public void rolesSerialization() throws SerializationException, DeserializationException {
        Role[] roles = {Role.MAFIA, Role.CITIZEN, Role.DOCTOR};

        for (Role r : roles) {
            byte b = Role.serialize(r);
            assertEquals(r, Role.deserialize(b));
        }
    }

    @Test
    public void phaseSerialization() throws SerializationException, IOException, DeserializationException {
        GamePhase[] phases = { new TestPhase(), new VotePhase(), new MafiaPhase(), new DoctorPhase() };

        for (GamePhase phase : phases) {
            DataOutputStream dout = getOutput();
            byte b = GamePhaseSerializer.serialize(phase.getClass());
            dout.write(b);
            assertEquals(phase.getClass(), GamePhaseSerializer.deserialize(getInput()).getClass());
        }
    }

    @Test
    public void playerSettingSerialization() throws SerializationException, IOException, DeserializationException {
        PlayerSettings example = new PlayerSettings(Role.DOCTOR, "Bob");

        DataOutputStream dout = getOutput();
        example.serialize(dout);
        PlayerSettings deserialized = PlayerSettings.deserialize(getInput());
        assertEquals(example.getRole(), deserialized.getRole());
        assertEquals(example.name, deserialized.name);
    }


    @Test
    public void settingSerialization() throws SerializationException, IOException, DeserializationException {
        List<PlayerSettings> players = Arrays.asList(
                new PlayerSettings(Role.DOCTOR, "Vlad molodec"),
                new PlayerSettings(Role.MAFIA, "Gaev petuh"),
                new PlayerSettings(Role.CITIZEN, "Kek")
        );
        List<GamePhase> phases = Arrays.asList(
                new TestPhase(),
                new VotePhase(),
                new MafiaPhase(),
                new DoctorPhase()
        );

        Settings s = new Settings(phases, players);

        DataOutputStream dout = getOutput();
        s.serialize(dout);
        Settings deserialized = Settings.deserialize(getInput());

        assertEquals(s.phases.size(), deserialized.phases.size());
        assertEquals(s.getPlayerSettings().size(), deserialized.getPlayerSettings().size());
        for (int i = 0; i < s.phases.size(); i++) {
            assertEquals(s.phases.get(i).getClass(), deserialized.phases.get(i).getClass());
        }

        for (int i = 0; i < s.getPlayerSettings().size(); i++) {
            assertEquals(s.getPlayerSettings().get(i).getRole(), deserialized.getPlayerSettings().get(i).getRole());
            assertEquals(s.getPlayerSettings().get(i).name, deserialized.getPlayerSettings().get(i).name);
        }
    }

    @Test
    void testPhaseStateSerialization() throws SerializationException, DeserializationException {
        final int SUM = 533;

        new TestPhaseServer(null).serializeGameState(getOutput(), new TestPhaseGameState(SUM));
        TestPhaseGameState d = (TestPhaseGameState) new TestPhaseClient().deserializeGameState(getInput());

        assertEquals(SUM, d.getSum());
    }

    @Test
    void testPickStatePickedSerialization() throws SerializationException, DeserializationException {
        final int PICKED = 3;
        PickState.pickedPickState(PICKED).serialize(getOutput());
        PickState p = PickState.deserialize(getInput());

        assertTrue(p.end);
        assertEquals(PICKED, p.getPicked());
    }

    @Test
    void testPickStatePicksSerialization() throws SerializationException, DeserializationException {
        final int[] PICKS = {1,2,3};
        PickState.picksPickState(PICKS).serialize(getOutput());
        PickState p = PickState.deserialize(getInput());

        assertFalse(p.end);
        assertArrayEquals(PICKS, p.getPicks());
    }

    @Test
    void votePhaseKilledStateSerialization() throws SerializationException, DeserializationException {
        final int KILLED = 533;

        VotePhaseGameState s = new VotePhaseGameState();
        s.end = true;
        s.killedPlayer = KILLED;

        new VotePhaseServer(null).serializeGameState(getOutput(), s);
        VotePhaseGameState d = (VotePhaseGameState) new VotePhaseClient().deserializeGameState(getInput());

        assertTrue(d.end);
        assertEquals(KILLED, d.killedPlayer);
    }

    @Test
    void votePhaseChooseStateSerialization() throws SerializationException, DeserializationException {
        final boolean[] CANT = { true, false, true, true};


        VotePhaseGameState s = new VotePhaseGameState();
        s.end = false;
        s.cantChoose = CANT;

        new VotePhaseServer(null).serializeGameState(getOutput(), s);
        VotePhaseGameState d = (VotePhaseGameState) new VotePhaseClient().deserializeGameState(getInput());

        assertFalse(d.end);
        assertArrayEquals(CANT, d.cantChoose);
    }
}
