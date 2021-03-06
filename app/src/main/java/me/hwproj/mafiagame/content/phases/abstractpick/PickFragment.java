package me.hwproj.mafiagame.content.phases.abstractpick;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import me.hwproj.mafiagame.R;
import me.hwproj.mafiagame.gameflow.Client;
import me.hwproj.mafiagame.gameflow.Player;
import me.hwproj.mafiagame.gameplay.Role;
import me.hwproj.mafiagame.phase.PhaseFragment;
import me.hwproj.mafiagame.util.table.TablePick;

/**
 * This fragments helps to implement a phase, where players of one role select one
 * player together.
 */
abstract public class PickFragment extends PhaseFragment {
    private final boolean pickSelfRole;
    private final int[] thisRolePlayers;
    private int thisPlayerNumber = -1;
    private TablePick table;
    private int currentPick = -1;
    private boolean notYourTurn;

    protected boolean isNotYourTurn() {
        return notYourTurn;
    }

    protected PickFragment(Client client, Role pickersRole, boolean pickSelfRole) {
        super(client);
        this.pickSelfRole = pickSelfRole;

        if (client.getGameData().players.get(client.thisPlayerId()).role != pickersRole) {
            notYourTurn = true;
            thisRolePlayers = null;
            return;
        }

        List<Integer> thisRoleIds = new ArrayList<>();
        for (int i = 0; i < client.playerCount(); i++) {
            Player p = client.getGameData().players.get(i);
            if (p.role == pickersRole && !p.dead) {
                if (i == client.thisPlayerId()) {
                    thisPlayerNumber = thisRoleIds.size();
                }
                thisRoleIds.add(i);
            }
        }

        thisRolePlayers = new int[thisRoleIds.size()];
        for (int i = 0; i < thisRolePlayers.length; i++) {
            thisRolePlayers[i] = thisRoleIds.get(i);
        }
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.generic_pick, container, false);
        Button pickFinal = view.findViewById(R.id.pickFinal);

        if (isNotYourTurn()) {
            pickFinal.setVisibility(View.GONE);
            TextView text = view.findViewById(R.id.pickNotYourTurn);
            text.setText(getString(R.string.not_your_turn));
            return view;
        }

        table = new TablePick(getContext(), client.getGameData(),
                view.findViewById(R.id.pickTable), thisRolePlayers.length);

        for (int i = 1; i < thisRolePlayers.length; i++) {
            table.setEnablePickingColumn(i, false);
        }

        if (!pickSelfRole) {
            for (int i : thisRolePlayers) {
                table.setEnablePickingRow(i, false);
            }
        }

        for (int i = 0; i < client.playerCount(); i++) {
            if (client.getGameData().players.get(i).dead) {
                table.setEnablePickingRow(i, false);
            }
        }

        table.setColumnListener(0, pick -> {
            currentPick = pick;
            sendPickAction(new PickAction(currentPick, false, thisPlayerNumber));
        });

        pickFinal.setVisibility(View.VISIBLE);
        pickFinal.setOnClickListener(v -> {
            if (currentPick != -1) {
                sendPickAction(new PickAction(currentPick, true, thisPlayerNumber));
            }
        });

        return view;
    }

    //    @Override
//    public void processGameState(GameState state) {
//
//    }

    protected void processPickedState(PickState data) {
        if (data.end) {
            Log.d("pick", "processPickedState: end");
            onPickComplete(data.pickedPlayer);
            return;
        }

        if (isNotYourTurn()) {
            return;
        }

        int metThisPlayer = 0;
        for (int i = 0; i < thisRolePlayers.length; i++) {
            int playerId = thisRolePlayers[i];
            if (playerId != client.thisPlayerId()) {
                table.setColumnPick(i + 1 - metThisPlayer, data.picks[i]);
            } else {
                metThisPlayer = 1;
            }
        }
    }

    protected abstract void onPickComplete(int pickedPlayer);

    protected abstract void sendPickAction(PickAction pickPhasePlayerAction);
}
