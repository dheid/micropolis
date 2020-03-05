package micropolisj.engine;

/**
 * The listener interface for receiving miscellaneous events that occur
 * in the Micropolis city.
 * Use the Micropolis class's addListener interface to register an object
 * that implements this interface.
 */
public interface CityListener
{
    void cityMessage(MicropolisMessage message, CityLocation loc);

    void citySound(Sound sound, CityLocation loc);

    /**
     * Fired whenever the "census" is taken, and the various historical
     * counters have been updated. (Once a month in game.)
     */
    default void censusChanged() {}

	/**
     * Fired whenever resValve, comValve, or indValve changes.
     * (Twice a month in game.)
     */
	default void demandChanged() {}

	/**
     * Fired whenever the city evaluation is recalculated.
     * (Once a year.)
     */
    default void evaluationChanged() {}

	/**
     * Fired whenever the mayor's money changes.
     */
    void fundsChanged();

    /**
     * Fired whenever autoBulldoze, autoBudget, noDisasters,
     * or simSpeed change.
     */
    void optionsChanged();
}
