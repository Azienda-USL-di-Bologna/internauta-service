package it.bologna.ausl.internauta.service.schedulers.workers.logoutmanager;

import java.io.Serializable;
import java.time.ZonedDateTime;

public class ConnectedPersona implements Serializable{
        private Integer id;
        private ZonedDateTime lastSeen;
        private String fromUrl;

        public ConnectedPersona() {
        }
        
        public ConnectedPersona(Integer id, ZonedDateTime lastSeen, String fromUrl) {
            this.id = id;
            this.lastSeen = lastSeen;
            this.fromUrl = fromUrl;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public ZonedDateTime getLastSeen() {
            return lastSeen;
        }

        public void setLastSeen(ZonedDateTime lastSeen) {
            this.lastSeen = lastSeen;
        }

        public String getFromUrl() {
            return fromUrl;
        }

        public void setFromUrl(String fromUrl) {
            this.fromUrl = fromUrl;
        }
    }