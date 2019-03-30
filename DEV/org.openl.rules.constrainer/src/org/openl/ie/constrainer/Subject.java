package org.openl.ie.constrainer;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
import java.util.Collection;

import org.openl.ie.tools.FastVector;

/**
 * An interface for the subject in the observer-subject (or subscriber-publisher) design pattern.
 */
public interface Subject extends Undoable {
    /**
     * Returns all dependents of this subject.
     */
    public Collection allDependents();

    /**
     * Attaches an observer to this subject.
     */
    public void attachObserver(Observer observer);

    /**
     * Detaches an observer from this subject.
     */
    public void detachObserver(Observer observer);

    /**
     * Undo helper: attaches an observer to this subject.
     */
    public void forcedAttachObserver(Observer observer);

    /**
     * Undo helper: detaches an observer from this subject.
     */
    public void forcedDetachObserver(Observer observer);

    /**
     * Undo helper: sets the publisher mask for this subject.
     */
    public void forcePublisherMask(int mask);

    /**
     * Sets the condition 'this subject is in the propagation process' to the given value.
     */
    public void inProcess(boolean value);

    /**
     * Notify the observers about an event. Only the observers interested in this event are notified.
     */
    public void notifyObservers(EventOfInterest interest) throws Failure;

    /**
     * Propagate changes made to this subject notifying the observers.
     */
    public void propagate() throws Failure;

    /**
     * Appends the mask to the publisher mask for this subject.
     */
    public void publish(int mask);

    /**
     * Returns the publisher mask for this subject.
     */
    public int publisherMask();

    /**
     * Sets the publisher mask for this subject.
     */
    public void publisherMask(int mask);

    /**
     * Reattaches an observer to this subject.
     */
    public void reattachObserver(Observer observer);

    /**
     * Will trace this subject every time when it has been modified.
     */
    public void trace();

    /**
     * Will trace this subject every time when the event of the "event_type" happens.
     *
     * @param event_type EventOfInterest.MAX or EventOfInterest.MIN or EventOfInterest.VALUE.
     */
    public void trace(int event_type);

} // ~Subject
