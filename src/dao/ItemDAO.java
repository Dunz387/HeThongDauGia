package dao;
import model.Item.Item;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO extends EntityDAO<Item> {
    //Tìm theo tên:
    public List<Item> searchByName(String keyword){
        List <Item> result = new ArrayList<>();

        for (Item item: entities){
            if (item.getName().toLowerCase().contains(keyword.toLowerCase())){
                result.add(item);
            }
        }
        return result;
    }
    //Tìm hàng còn sẵn
    public List<Item> getAvailableItems(){
        List<Item> result = new ArrayList<>();

        for (Item item: entities){
            if (item.isAvailable()){
                result.add(item);
            }
        }
        return result;
    }
}
