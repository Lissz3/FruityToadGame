package com.isabelrosado.fruitytoad.Sprites.TileObjects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapObject;
import com.isabelrosado.fruitytoad.FruityToad;
import com.isabelrosado.fruitytoad.Screens.PlayScreen;

public class CheckPoint extends InteractiveTileObject {
    public CheckPoint(FruityToad game, PlayScreen screen, MapObject object) {
        super(game, screen, object);
        fixture.setUserData(this);
        setCategoryFilter(FruityToad.CHECKPOINT_BIT);
    }

    @Override
    public void onHeadHit() {
        use();
    }

    public void use() {
        if (screen.getHud().getScore() >= 5) {
            screen.getHud().getLblWarning().setVisible(false);
            screen.getHud().setPaused(true);
            game.setFinalScore(game.getFinalScore()+screen.getHud().getScore());
            Gdx.app.log("FinalScore", game.getFinalScore()+"");
            if (screen.getHud().getGameLevel() != game.getTotalLevels()) {
                screen.getHud().getWinScreen().setVisible(true);
            } else {
                screen.getHud().getSaveScoreScreen().setVisible(true);
            }

        } else {
            screen.getHud().getLblWarning().setVisible(true);
        }
    }
}