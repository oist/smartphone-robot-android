package jp.oist.abcvlib.claplearn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.math3.distribution.EnumeratedRealDistribution;

/**
 * Class to allow for probability sampling from categories, each with a defined probability of selection. Items are added
 * to the sampler with a given sampling value - either the default supplied - and this value is used to sample
 * from the underlying items in a probabilistic way. The true probability of selection is the defined sampling value
 * of an item divided by the total sampling value of all items.
 * @author Greg Cope
 *
 * @param <T>
 */
public class CategoricalSampler<T> {

    private Map<T, Double> samplingValues = new HashMap<T, Double>();
    private double defaultSamplingValue;
    private double totalSamplingValue = 0;

    /**
     * Constructs a new sampler object with a default probability of added items of 0.1
     */
    public CategoricalSampler(){
        this(0.0);
    }

    /**
     * Constructs a new sampler object with the defined default probability.
     * @param defaultSamplingValue
     */
    public CategoricalSampler(double defaultSamplingValue){
        this.defaultSamplingValue = defaultSamplingValue;
    }

    /**
     * Adds the given parameter item, using the default probability value.
     * @param item
     */
    public void addItem(T item){
        addItem(item, defaultSamplingValue);
    }

    /**
     * Adds the given item to this sampler with the given value. If
     * the item already exists, it is replaced with the given value
     * @param item
     * @param value
     */
    public void addItem(T item, double value){
        if ( !samplingValues.containsKey(item) ){
            samplingValues.put(item, value);
            totalSamplingValue += value;
        }else{
            Double d = samplingValues.get(item);
            totalSamplingValue -= d;
            samplingValues.put(item, value);
            totalSamplingValue += value;
        }
    }

    /**
     * Increments an items value by incrementor. If the value does
     * not exist within this sampler, it is added with incrementor inital value
     * @param item
     * @param incrementor
     */
    public void incrementSamplingValue(T item, double incrementor){

        if ( !samplingValues.containsKey(item) ){
            addItem(item, incrementor);
        }else{
            double val = samplingValues.get(item);
            samplingValues.put(item, val+incrementor);
            totalSamplingValue += incrementor;
        }
    }

    /**
     * Returns the sampling value of the parameter item, or -1 if the item is not part of this CategoricalSampler.
     * @param item
     * @return
     */
    public double getSamplingValue(T item){
        if ( !samplingValues.containsKey(item) ){
            return -1;
        }
        return samplingValues.get(item);
    }


    /**
     * Sets the sampling value of the given item.
     * @param item
     * @param value
     */
    public void setSamplingValue(T item, double value){
        addItem(item, value);
    }

    /**
     * Samples a discrete value from the underlying distribution. This method runs in linear time
     * @return The sampled item.
     */
    public T sample(){

        if ( samplingValues.size() == 0 ){
            throw new IllegalStateException("Empty set");
        }

        double val = Math.random() * totalSamplingValue;
        Iterator<T> items = samplingValues.keySet().iterator();
        double total = 0;
        T returner = null;

        while ( items.hasNext() ){
            T item = items.next();
            double prop = samplingValues.get(item);
            if ( val >= total && val < total + prop ){
                returner = item;
                break;
            }
            total += prop;
        }
        return returner;
    }

    @Override
    public String toString(){

        return samplingValues.toString();

    }
}
