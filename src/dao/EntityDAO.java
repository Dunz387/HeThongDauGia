package dao;

import model.BaseClass.Entity;
import java.util.*;
import java.util.List;
import java.util.ArrayList;

public class EntityDAO <T extends Entity> {
    protected List<T> entities;
    public EntityDAO(){
        entities = new ArrayList<>();
    }
    // Thêm thực thể Entity
    public void add (T entity){
        if (entity != null){
            entities.add(entity);
        }
    }
    // Nhận hết thực thể Entity
    public List<T> getAll(){
        return entities;
    }
    //Tìm bằng ID
    public T findById(String id){
        for (T entity: entities){
            if (entity.getID().equals(id)){
                return entity;
            }
        }
        return null;
    }
    // Xoá
    public void remove (T entity){
        entities.remove(entity);
    }
}
