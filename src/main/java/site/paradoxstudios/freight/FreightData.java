package site.paradoxstudios.freight;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class FreightData extends SavedData {
    public static final String KEY="freightlogistics_companies";
    public final Map<UUID, Company> companies=new LinkedHashMap<>();
    public final Map<Integer, Contract> contracts=new LinkedHashMap<>();
    public int nextContractId=1;
    public static FreightData get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(FreightData::new,FreightData::load),KEY);
    }
    public static FreightData load(CompoundTag tag,HolderLookup.Provider provider){
        FreightData data=new FreightData();
        data.nextContractId=Math.max(1,tag.getInt("NextContractId"));
        ListTag companyTags=tag.getList("Companies",Tag.TAG_COMPOUND);
        for(int i=0;i<companyTags.size();i++){
            CompoundTag c=companyTags.getCompound(i);
            try {
                UUID owner=c.getUUID("Owner");
                Company entry=new Company(owner,c.getString("Name"),c.getInt("Balance"),c.getLong("Depot"));
                data.companies.put(owner,entry);
            } catch(Exception ignored){}
        }
        ListTag orderTags=tag.getList("Contracts",Tag.TAG_COMPOUND);
        for(int i=0;i<orderTags.size();i++){
            CompoundTag c=orderTags.getCompound(i);
            try {
                Contract contract=new Contract(c.getInt("Id"),c.getString("Item"),c.getInt("Count"),c.getInt("Reward"));
                if(c.hasUUID("AssignedTo")) contract.assignedTo=c.getUUID("AssignedTo");
                contract.completed=c.getBoolean("Completed");
                data.contracts.put(contract.id,contract);
            }catch(Exception ignored){}
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag out,HolderLookup.Provider provider){
        out.putInt("NextContractId",nextContractId);
        ListTag companyTags=new ListTag();
        for(Company c:companies.values()){
            CompoundTag tag=new CompoundTag();
            tag.putUUID("Owner",c.owner);tag.putString("Name",c.name);
            tag.putInt("Balance",c.balance);tag.putLong("Depot",c.depot);
            companyTags.add(tag);
        }
        out.put("Companies",companyTags);
        ListTag contractsTag=new ListTag();
        for(Contract c:contracts.values()){
            CompoundTag tag=new CompoundTag();
            tag.putInt("Id",c.id);tag.putString("Item",c.item);
            tag.putInt("Count",c.count);tag.putInt("Reward",c.reward);
            tag.putBoolean("Completed",c.completed);
            if(c.assignedTo!=null) tag.putUUID("AssignedTo",c.assignedTo);
            contractsTag.add(tag);
        }
        out.put("Contracts",contractsTag);
        return out;
    }
    public Company company(UUID player){return companies.get(player);}
    public Company create(UUID id,String name){
        Company c=new Company(id,name,2500,0L);companies.put(id,c);setDirty();return c;
    }
    public Contract issue(String item,int count,int reward){
        Contract c=new Contract(nextContractId++,item,count,reward);
        contracts.put(c.id,c);setDirty();return c;
    }
    public static final class Company {
        public final UUID owner;
        public final String name;
        public int balance;
        public long depot;
        Company(UUID owner,String name,int balance,long depot) {
            this.owner=owner;this.name=name;this.balance=balance;this.depot=depot;
        }
    }
    public static final class Contract {
        public final int id;
        public final String item;
        public final int count;
        public final int reward;
        public UUID assignedTo;
        public boolean completed;
        Contract(int id,String item,int count,int reward){
            this.id=id;this.item=item;this.count=count;this.reward=reward;
        }
    }
}
