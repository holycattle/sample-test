# -*- mode: ruby -*-
# vi: set ft=ruby :

#Find Vagrant root directory so you can run 'vagrant up' anywhere
root_dir = Dir.pwd
until Dir.entries(root_dir).include? 'Vagrantfile'
  root_dir = File.expand_path('..', root_dir)
end
Dir.chdir(root_dir)

Vagrant.configure(2) do |config|
  config.vm.provider "virtualbox" do |v, override|
    override.vm.box = "ubuntu1404"
    override.vm.hostname = "scala-test"
    override.vm.network "private_network", ip: "192.168.33.101"
    override.vm.synced_folder "data", "/home/vagrant/data", :nfs => true
    override.vm.synced_folder ".", "/home/vagrant/scala-test", :nfs => true
    override.vm.synced_folder ".", "/var/www/scala-test", :nfs => true
  
    override.vm.network :forwarded_port, guest: 8080, host: 8080 #API/backend
    override.ssh.forward_x11 = true

    override.vm.provider :virtualbox do |vb|
      vb.customize ["modifyvm", :id, "--memory", 2048]
    end

    override.vm.provision :ansible do |a|
      a.sudo = true
      a.playbook = "ansible/all.yml"
      a.inventory_path = "ansible/hosts"
      a.limit = 'all'
    end

    #HACK -- I accidentally deleted my .vagrant folder and shit blew up
    override.ssh.private_key_path = "~/.ssh/id_rsa"
    override.ssh.forward_agent = true
  end
end
