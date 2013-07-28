/**
 * EntityConcern.aj
 * 
 * Created on 27.07.2013
 */

package at.pria.koza.harmonic;


/**
 * <p>
 * {@code EntityConcern} provides inter-type declarations and advices that ensure consistent behavior for all
 * {@linkplain Entity entities}.
 * </p>
 * 
 * @version V1.0 27.07.2013
 * @author SillyFreak
 */
public aspect EntityConcern {
    private Engine Entity.harmonic$engine;
    private int    Entity.harmonic$id;
    
    public void Entity.setEngine(Engine engine, int id) {
        harmonic$engine = engine;
        harmonic$id = id;
    }
    
    public Engine Entity.getEngine() {
        return harmonic$engine;
    }
    
    public int Entity.getId() {
        return harmonic$id;
    }
    
    /**
     * <p>
     * Matches the initialization of an {@link Entity} class.
     * </p>
     * 
     * TODO an improvement would be to determine here if the constructor is the last one being called belonging to
     * an {@code Entity} class.
     * 
     * @param engine the {@link Engine} given as the first argument of the constructor
     * @param entity the {@link Entity} being initialized
     */
    pointcut newEntity(Engine engine, Entity entity):
        initialization(Entity+.new(Engine, ..)) && this(entity) && args(engine, ..);
    
    /**
     * <p>
     * Advises {@link Entity} constructors to register the entity in their {@link Engine}. This happens before any
     * other initialization of the entity class, i.e. the constructor can access engine and id via
     * {@link Entity#getEngine() getEngine()} and {@link Entity#getId() getId()}. Specifically, it happens directly
     * after the most specific non-entity superclass constructor was run.
     * </p>
     * 
     * TODO this is implemented by checking whether the engine was already set; the code is generated even for
     * constructors calling a {@code this()} or {@code super()} entity constructors. Strictly speaking, this does
     * not advise only those pointcuts where the code belongs.
     * 
     * @param engine the {@link Engine} given as the first argument of the constructor
     * @param entity the {@link Entity} being initialized
     */
    before(Engine engine, Entity entity): newEntity(engine, entity) {
        if(entity.harmonic$engine == null) engine.put(entity);
    }
    
    /**
     * <p>
     * Declares it as an error to call {@link Entity#setEngine(Engine, int) setEngine()} by yourself. This is done
     * by the {@link Engine#put(Entity) put()} method, and should be done nowhere else.
     * </p>
     */
    declare error: !within(Engine) && call(public void Entity.setEngine(Engine, int)):
        "setEngine() must not be called explicitly; it is called by Engine";
    
    /**
     * <p>
     * Declares it as an error to call {@link Engine#put(Entity) put()} by yourself. This is done by this aspect,
     * and should be done nowhere else.
     * </p>
     * <p>
     * Note that projects that do not process this library's aspects have to call the method themselves; but as
     * they don't process this aspect, they also don't get an error for doing so.
     * </p>
     */
    declare error: !within(EntityConcern) && call(public void Engine.put(Entity)):
        "put() must not be called explicitly; it is called by an advice of Entity";
    
    /**
     * <p>
     * Declares it as an error to declare an entity constructor that does not use an Engine as its first parameter.
     * This parameter is necessary for the advice above to work.
     * </p>
     * <p>
     * Note that projects that do not process this library's aspects may do this another way; but as they don't
     * process this aspect, they also don't get an error for doing so.
     * </p>
     */
    declare error: execution(Entity+.new(..)) && !execution(Entity+.new(Engine, ..)):
        "Entity constructors must provide an engine as their first argument";
}
