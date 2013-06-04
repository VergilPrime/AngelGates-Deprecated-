##Overview##

AngelGates allows people to create gates that cover any amount of distance, possibly connecting different worlds, and share those gates with members of a network.

Members of a network can add more gates to that network, and owners can manage that network's members.

If you have Towny installed, Towns can be owners or members, and if you have Vault installed, groups can also be owners or members.

##Commands##

Here are the commands that manipulate how AngelGates operates. You can use either `/ag` or `/angelgates`, tho we will use `/ag` here for brevity.

_You can prefix names with `t:` or `g:` to use Towny towns and permission groups respectively._

`/ag help [command]` - View help in-game. The command is optional.

`/ag reload` - Reload AngelGates from disk. Requires `angelgates.admin.reload` permission.

`/ag info [name]` - Find out info about yourself, or about a given name if provided. The name is optional.

`/ag netinfo <network>` - Find out info about a given network.

`/ag setnetworks <name> <amount>` - Set the limit of networks for a given name to a given amount. Set this to `-1` to allow infinite networks, and `0` to not allow any. Requires `angelgates.admin` permission.

`/ag addnetworks <name> <amount>` - Increase the limit of networks for a given name by a given amount. Requires `angelgates.admin.addnetworks` permission.

`/ag setowner <net> <name>` - Set the given name as the owner for the given network. The previous owner will be demoted to member. Can only be run by that network's owner, or with the `angelgates.admin.setowner` permission.

`/ag addmember <net> <name>` - Add a given name as a member to a given network. Can only be run by that network's owner, or with the `angelgates.admin.addmember` permission.

`/ag remmember <net> <name>` - Remove a given name as a member from a given network. Can only be run by that network's owner, or with the `angelgates.admin.remmember` permission.

##Permissions##

There aren't many permissions, as this is built to be as permissible as possible, but there are some admin overrides, as well as command permissions. 

_Parent nodes such as `angelgates.command` imply all child nodes, such as `angelgates.command.reload`, are given._

`angelgates.command.<commandname>` - Permit use of a given command regardless of membership or ownership status. See commands above for possible command names.

`angelgates.admin.create` - Allow creation of any network or gate.

`angelgates.admin.destroy` - Allow destruction of any gate.

`angelgates.admin.use` - Allow activation (selection of destination) or opening of any gate.

##Signs##

When creating a portal, the first line of the sign must contain the network you are adding the portal to. The second line is the name of the portal itself.

When a portal is created, you can hit the sign to display and switch between the possible destinations in that network. Hitting the switch will toggle the gate's open state, once a destination is selected.

##Gate Design##

You build a gate via placing obsidian in the following format:

     XX 
    X  X 
    X  X
    X  X
     XX

Then you place a sign in the middle of either of the vertical walls, following the above section of signs.

##Custom Gates##

You can create as many gate formats as you want, the gate layouts are stored in plugins/AngelGates/gates/
The .gate file must be laid out a specific way, the first lines will be config information, and after a blank line you will lay out the gate format. Here is the default nethergate.gate file:

    portal-open=90
    portal-closed=0
    X=49
    -=49

     XX 
    X..X
    -..-
    X*.X
     XX 

In this example, portal-open/closed are used to define the material in the gate when it is open or closed. 
"X" and "-" are used to define block "types" for the layout (Any single-character can be used, such as "#"). You can use as many character definitions as you need, and the gate can be any dimensions.

In the gate format, you can see we use "X" to show where obsidian must be, "-" where the controls (Button/sign) are.
You will also notice a "*" in the gate layout, this is the "exit point" of the gate, the block at which the player will teleport in front of.

You can supply block data in the format `X=35:15` which would only allow black wool. If no data is supplied any version of a block will work (any color of wool, for example).

##Credits##

`Dinnerbone` - Original hMod version.

`TheDgtl` - Bukkit port of the hMod version. (see [here][thedgtl] for source)
[thedgtl]: http://github.com/TheDgtl/Stargate-Bukkit

`VergilPrime` from [`Angel's Reach`][reach] - Coder motivation ;)
[reach]: http://www.angels-reach.net/
