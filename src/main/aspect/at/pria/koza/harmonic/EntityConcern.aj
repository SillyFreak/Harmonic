/**
 * EntityConcern.aj
 * 
 * Created on 27.07.2013
 */

package at.pria.koza.harmonic;


/**
 * <p>
 * {@code EntityConcern}
 * </p>
 * 
 * @version V0.0 27.07.2013
 * @author SillyFreak
 */
public aspect EntityConcern {
    private Engine Entity.engine;
    private int    Entity.id;
    
    public void Entity.setEngine(Engine engine, int id) {
        this.engine = engine;
        this.id = id;
    }
    
    public Engine Entity.getEngine() {
        return engine;
    }
    
    public int Entity.getId() {
        return id;
    }
    
    pointcut newEntity(Engine engine, Entity entity):
        initialization(Entity+.new(Engine, ..)) && this(entity) && args(engine, ..)
            && if(!Entity.class.isAssignableFrom(thisJoinPointStaticPart.getSourceLocation().getWithinType().getSuperclass()));
    
    before(Engine engine, Entity entity): newEntity(engine, entity) {
        engine.put(entity);
        System.out.println("before " + thisJoinPointStaticPart);
    }
    
    declare error: !within(EntityConcern) && call(public void Engine.put(Entity)):
        "put() must not be called explicitly; it is called by advice of entity";
    
    declare error: execution(Entity+.new(..)) && !execution(Entity+.new(Engine, ..)):
        "Entity constructors must provide an engine as their first argument";
}
