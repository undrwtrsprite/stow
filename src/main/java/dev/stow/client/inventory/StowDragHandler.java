package dev.stow.client.inventory;

/** Lets optional input integrations yield only while Stow owns a gesture. */
public interface StowDragHandler {
    boolean stow$ownsDrag();
}
