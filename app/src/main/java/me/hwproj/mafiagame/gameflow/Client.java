package me.hwproj.mafiagame.gameflow;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.LinkedList;
import java.util.Queue;

import me.hwproj.mafiagame.networking.ClientSender;
import me.hwproj.mafiagame.networking.FullGameState;
import me.hwproj.mafiagame.networking.MetaInformation;
import me.hwproj.mafiagame.networking.ServerNetworkPackage;
import me.hwproj.mafiagame.phase.GamePhase;
import me.hwproj.mafiagame.phase.GameState;
import me.hwproj.mafiagame.phase.PhaseFragment;
import me.hwproj.mafiagame.phase.PlayerAction;

/**
 * Mostly a callback for PhaseFragment.
 *
 * Also stores ServerNetworkPackage-s received from server (and passed to Client
 * from ClientGame) and manipulates ClientGame through ClientCallbacks or
 * LiveData currentPhaseNumber.
 */
public class Client {

    private final int thisPlayer;
    private final ClientCallbacks callbacks;


    /**
     * Send action to server.
     * This is called by phases.
     * @param action
     */
    public void sendPlayerAction(PlayerAction action) {
        if (sender != null) { // TODO
            sender.sendPlayerAction(action);
        }
    }

    // ------------- from ClientGame -------------

    /**
     * Starts next phase and gets it's fragment.
     * Called from ClientGame.
     * @return next phase's fragment
     */
    public PhaseFragment nextPhaseFragment() {
        currentGameState.nextPhase();
        callbacks.setToolbarText(currentGameState.getCurrentPhase().toolbarText());
        return currentGameState.getCurrentPhase().createFragment(this);
    }

    /**
     * Called by ClientGame when this player is killed
     */
    @SuppressWarnings("EmptyMethod")
    public void onThisPlayerKilled() {
        // currently does nothing
        // cant do much since don't have Context
        // might need this method later
    }

    /**
     * Receive a package from server
     * @param pack package to handle
     */
    public void receivePackage(ServerNetworkPackage pack) {
        Log.d("qwe", "receivePackage: meta: " + pack.isMeta());
        packageQueue.add(pack);
        packageData.postValue(pack); // mostly to call listeners
    }

    // -------------------------------------------

    private void receiveState(FullGameState state) {
        Log.d("qwe", "receiveState: received");
        getGameData().update(state);
        callbacks.handleGameState(state.getPhaseState());
    }

    private void handlePackage(ServerNetworkPackage pack) {
        Log.d("qwe", "handlePackage: handle meta " + pack.isMeta());
        if (pack.isMeta()) {
            Log.d("qwe", "handlePackage: META PACKAGE");
            receiveMeta(pack.getMeta());
        } else {
            receiveState(pack.getGameState());
        }
    }

    private void startNextPhase(int number) {
        currentPhaseNumber.setValue(number);
    }

    private void receiveMeta(MetaInformation metaInformation) {
        Log.d("Ok", "receiveMeta: accepting meta");
        if (metaInformation.what() == MetaInformation.NEXT_PHASE) {
            startNextPhase(metaInformation.getNumber());
        } else if (metaInformation.what() == MetaInformation.END_GAME) {
            String finishMessage;
            if (metaInformation.getGoodWon()) {
                finishMessage = "Good won";
            } else {
                finishMessage = "Bad won";
            }
            finishGame(finishMessage);
        }
    }

    private void finishGame(String finishMessage) {
        callbacks.finishGame(finishMessage);
    }

    private final ClientGameData currentGameState;
    private final ClientSender sender;

    private final MutableLiveData<GameState> latestGameState = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentPhaseNumber = new MutableLiveData<>();
    private final MutableLiveData<ServerNetworkPackage> packageData = new MutableLiveData<>();
    private final Queue<ServerNetworkPackage> packageQueue = new LinkedList<>();

    public Client(ClientSender sender, Settings settings, int thisPlayer, ClientCallbacks callbacks) {
        this.callbacks = callbacks;
        currentGameState = new ClientGameData();
        for (GamePhase p : settings.phases) {
            currentGameState.phases.add(p.getClientPhase());
        }
        for (PlayerSettings p : settings.getPlayerSettings()) {
            currentGameState.players.add(p.constructPlayer());
        }

        currentPhaseNumber.setValue(0);

        packageData.observeForever(pack -> {
            while (!packageQueue.isEmpty()) {
                handlePackage(packageQueue.poll());
            }
        });


        this.sender = sender;
        this.thisPlayer = thisPlayer;
    }

    // ------------- various getters -------------

    public GameState getLatestGameState() {
        return latestGameState.getValue();
    }

    public LiveData<Integer> getPhaseNumberData() {
        return currentPhaseNumber;
    }

    public ClientGameData getGameData() {
        return currentGameState;
    }

    public int playerCount() {
        return getGameData().players.size();
    }

    public int thisPlayerId() {
        return thisPlayer;
    }

    public Player thisPlayer() {
        return getGameData().players.get(thisPlayerId());
    }
}
