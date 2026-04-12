package model.BaseClass;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Entity {
    protected String id;
    protected LocalDateTime createdAt;

    public Entity(){
        this.id = UUID.randomUUID().toString(); //Lấy mã ID ngẫu nhiên
        this.createdAt = LocalDateTime.now(); //Thời gian thực
    }
    public String getID(){
        return id;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setId(String id){
        this.id = id;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}