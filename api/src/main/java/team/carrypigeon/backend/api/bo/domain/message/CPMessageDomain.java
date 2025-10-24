package team.carrypigeon.backend.api.bo.domain.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageDomain {
    private String domain;
    private String type;

    public static CPMessageDomain parseDomain(String messageDomain){
        int index = messageDomain.indexOf(":");
        String domain = messageDomain.substring(0, index);
        String type = messageDomain.substring(index+1);
        return new CPMessageDomain(domain,type);
    }

    public boolean isCore() {
        return domain.equals("Core");
    }

    public String toPOData(){
        return domain+":"+type;
    }
}