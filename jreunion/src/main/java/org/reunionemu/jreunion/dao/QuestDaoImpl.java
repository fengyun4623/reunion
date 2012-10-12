package org.reunionemu.jreunion.dao;


import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;

import org.reunionemu.jreunion.model.Quest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Lazy(true)
public class QuestDaoImpl implements QuestDao {

	private QuestList quests;
	
	@Autowired
	ApplicationContext context;
		
	@Value("${quest.resource.location}")
	Resource resource;
	
	@PostConstruct
	public void init() throws Exception{
		
		JAXBContext context = JAXBContext.newInstance(QuestListImpl.class);
		
		quests = (QuestList) context.createUnmarshaller().unmarshal(resource.getInputStream());
		
	}
	
	@Override
	public Quest findById(int id) {
		for(Quest quest: quests){
			if(quest.getId()==id){
				return quest;
			}
		}
		throw new IllegalStateException("Quest with id: "+id+" not found");
	}
	
}