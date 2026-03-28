public abstract class Entity {
    protected String id;
    protected String name;

    public Entity(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    //getters and setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract void displayInfo(); //display Object's info 
}
