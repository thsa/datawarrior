package com.actelion.research.table.view.card.animators;

/**
 * Defines an animation. Time is
 *
 */
public interface AnimatorInterface {

    public void initAnimation();

    /**
     *
     * @param timeInSeconds  time after start of animation
     * @return returns whether animation continues
     */
    public boolean animate(double timeInSeconds);

}
