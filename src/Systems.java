import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * 文件管理系统 主类
 *
 * @author liaijun
 *
 */
public class Systems {
    Scanner sc = new Scanner(System.in);// 从控制台读取数据

    public static SuperBlock sb = null;// 超级块 记录虚拟磁盘的总信息
    public static ArrayList<String> users;// 用户名数组;
    public static INode[] inodes = new INode[100];// i节点记录数据结构
    public static Object[] blocks = new Object[100];// 文件块的结构；
    public static String name = null;// 当前登录用户名
    public static INode now_inode = null;// 当前节点
    public static Object now_file = null;

    // public static INode father;//父节点
    // public static INode me;//自己的当前节点

    /**
     * @param args
     */
    public static void main(String[] args) {
        Systems sts = new Systems();
        sts.init();// 初始化数据；
        sts.login();

    }

    public void init() {
        users = (ArrayList<String>) FileTools.read("f:\\users.dat");
		/*
		 * if(null!=FileTools.read("f:\\users.dat")) { //inodes=(INode[])
		 * FileTools.read("f:\\users.dat"); }
		 */
        sb = (SuperBlock) FileTools.read("f:\\super.dat");
        if (null == sb || sb.getAlreadyuse() == 0) {
            for (int i = 0; i < 100; i++) {
                inodes[i] = new INode();
            }
            sb = new SuperBlock();
            for (int i = 0; i < 100; i++) {
                sb.setInode_free(i);
            }
            FileTools.write("f:\\super.dat", sb);
        }
        if (null == users) {
            users = new ArrayList<String>();// 存放整个文件系统
            users.add("admin");
            FileTools.write("f:\\users.dat", users);
        }

    }

    public void login() {

        System.out.println("***************欢迎使用该文件管理系统*************");
        System.out.println("请先登录->");
        name = sc.next();
        if (!this.isInNames(name)) {
            System.out.println("该用户名不存在！是否注册该用户？y/n");
            if ("y".equals(sc.next())) {
                if (regeist(name)) {
                    System.out.println(name + "注册成功！");
                    login();
                } else {
                    System.out.println("注册失败！");
                    System.exit(0);
                }

            } else {
                login();
            }

        } else {
            now_inode = getInode(name + "->");// 得到当前的inode
            now_file = blocks[now_inode.getAddress()];// 得到当前的目录
            System.out.println("登录成功");
            execute();
        }
    }

    /**
     * 命令执行的主方法
     */
    public void execute() {

        String commond = null;
        String cmd[] = null;// 操作命令数组 cmd[0] 操作的命令 cmd[1]操作的文件
		/*
		 * INode id = new INode();// 文件的节点 int fileNumber = 0;// 拥有文件的总数 int
		 * getNumber = 0;// 存储打开文件的节点索引号，或者说是虚拟的内存地址 int emptyNumber = 0;//
		 * 空的文件目录的索引
		 */// System.out.println(now_inode.getPath());

        while (true) {
            System.out.print(now_inode.getPath());
            commond = sc.nextLine();
            if (commond.equals(""))
                commond = sc.nextLine();
            cmd = commond.trim().split(" ");
            // 列举同一个人用户名的文件目录
            if (cmd[0].trim().equals("dir")) {
                int m = 0;
                System.out.println("文件名\t用户名\t地址\t文件长度\t只读1/可写2\t打开控制");
                if (now_file instanceof MyDirectory) {
                    MyDirectory now__real_file = (MyDirectory) now_file;
                    m = now__real_file.getTree().size();
                    if (m == 0) {
                        System.out.println("没有目录项");
                    } else {

                        Set<Integer> dir_inodes = now__real_file.getTree()
                                .keySet();
                        Iterator<Integer> iteratore = dir_inodes.iterator();
                        while (iteratore.hasNext()) {

                            Object file = blocks[now__real_file.getTree().get(
                                    iteratore.next())];
                            if (file instanceof MyDirectory) {
                                MyDirectory real_file = (MyDirectory) file;
                                INode real_inode = inodes[real_file
                                        .getInode_address()];
                                // "文件名\t用户名\t地址\t文件长度\t只读1/可写2\t打开控制\t创建时间"
                                System.out.println(real_file.getName() + "\t"
                                        + real_inode.getUsers() + "\t"
                                        + real_inode.getAddress() + "\t"
                                        + real_inode.getLength() + "B\t"
                                        + real_inode.getRight() + "\t"
                                        + real_inode.getState() + "\t"
                                        + real_inode.getModifytime());

                            } else {
                                MyFile real_file = (MyFile) file;
                                INode real_inode = inodes[real_file
                                        .getInode_address()];
                                System.out.println(real_file.getName() + "\t"
                                        + real_inode.getUsers() + "\t"
                                        + real_inode.getAddress() + "\t"
                                        + real_inode.getLength() + "B\t"
                                        + real_inode.getRight() + "\t"
                                        + real_inode.getState() + "\t"
                                        + real_inode.getModifytime());

                            }

                        }
                        System.out.println("文件个数---" + m);
                    }

                } else {
                    MyFile now__real_file = (MyFile) now_file;
                }

            }
            // 创建文件
            else if (cmd[0].equals("create")) {

                int index = getFreeInode();
                if (index != -1) {
                    MyFile my_file = new MyFile();
                    my_file.setName(cmd[1]);
                    INode inode = new INode();
                    inode.setFather(now_inode.getMe());
                    inode.setUsers(name);
                    inode.setMe(index);
                    inode.setModifytime();
                    if (inode.getFather() == -1) {
                        inode.setPath(name + "->");
                    } else {
                        inode.setPath(inodes[inode.getFather()].getPath()
                                + cmd[1] + "->");
                    }
                    inode.setRight(1);// 可写
                    inode.setState("open");
                    inode.setType(1);// 文件
                    inode.setAddress(index);
                    inodes[index] = inode;
                    my_file.setInode_address(index);
                    MyDirectory real_file = (MyDirectory) now_file;
                    blocks[index] = my_file;
                    real_file.getTree().put(index, index);
                    System.out.println(cmd[1] + "文件已经打开！请输入内容。。。#end结束输入");
                    StringBuffer content = new StringBuffer();
                    while (true) {
                        String tem = sc.nextLine();
                        if (tem.equals("#end")) {
                            System.out.println("文件输入结束");
                            break;// 文件输入结束
                        }
                        content.append(tem + "\r\n");
                    }
                    my_file.setSubstance(content.toString());
                    inodes[index].setLength(content.length());
                    inodes[index].setState("close");
                    System.out.println(cmd[1] + "文件已关闭！");
                    sb.setAlreadyuse(content.length());
                    sb.setInode_busy(index);
                } else {
                    System.out.println("inode申请失败！");
                }

            }
            // 创建文件目录
            else if (cmd[0].trim().equals("mkdir")) {
                int index = getFreeInode();
                if (index != -1) {
                    MyDirectory my_file = new MyDirectory();
                    my_file.setName(cmd[1]);
                    INode inode = new INode();
                    inode.setFather(now_inode.getMe());
                    inode.setUsers(name);
                    inode.setMe(index);
                    inode.setModifytime();
                    inode.setPath(now_inode.getPath() + cmd[1] + "->");
                    inode.setRight(1);// 可写
                    inode.setType(0);// 文件
                    inode.setAddress(index);
                    inodes[index] = inode;
                    my_file.setInode_address(index);

                    MyDirectory real_file = (MyDirectory) now_file;
                    blocks[index] = my_file;
                    real_file.getTree().put(index, index);
                    inodes[index].setLength(0);
                    sb.setInode_busy(index);

                } else {
                    System.out.println("inode申请失败！");
                }

            }
            // 删除文件的操作
            else if (cmd[0].trim().equals("delete")) {

                Object o = this.getFileByName(cmd[1]);
                if (null != o) {
                    if (o instanceof MyDirectory) {
                        MyDirectory o1 = (MyDirectory) o;

                        if (o1.getTree().size() == 0) {
                            int index = o1.getInode_address();
                            sb.setInode_free(index);
                            // 重置节点
                            inodes[index] = new INode();
                            // 重置数据块
                            blocks[o1.getInode_address()] = new Object();
                            // 在目录的tree中删除数据
                            MyDirectory file = (MyDirectory) now_file;
                            file.getTree().remove(index);

                            System.out.println(o1.getName() + "目录已删除！");
                        } else {
                            System.out.println(o1.getName() + "目录不为空！不可以删除");
                        }
                    } else if (o instanceof MyFile) {
                        MyFile o1 = (MyFile) o;

                        int index = o1.getInode_address();
                        // 设置超级快
                        sb.setInode_free(index);
                        sb.setFreeuse(inodes[index].getLength());
                        // 重置节点
                        inodes[index] = new INode();
                        // 重置数据块
                        blocks[o1.getInode_address()] = new Object();
                        // 在目录的tree中删除数据
                        MyDirectory file = (MyDirectory) now_file;
                        file.getTree().remove(index);

                        System.out.println(o1.getName() + "文件已删除！");

                    } else {
                        System.out.println(cmd[1] + "文件不存在！");
                    }
                }

            } else if (cmd[0].trim().equals("cd")) {
                if (".".equals(cmd[1])) {

                } else if ("..".equals(cmd[1])) {
                    if (now_inode.getFather() == -1) {
                        System.out.println("当前目录为根目录！");
                    } else {
                        MyDirectory now_directory = (MyDirectory) now_file;
                        now_inode = inodes[now_inode.getFather()];
                        now_file = blocks[now_inode.getAddress()];
                    }
                } else if (null != getFileByName(cmd[1])) {
                    Object o1 = getFileByName(cmd[1]);
                    if (o1 instanceof MyDirectory) {
                        MyDirectory o = (MyDirectory) o1;
                        now_file = o;
                        now_inode = inodes[o.getInode_address()];
                    } else {
                        System.out.println("输入的目录不存在，请检查！");
                    }

                } else {
                    System.out.println("输入的目录不存在，请检查！");
                }

            } else if (cmd[0].trim().equals("open")) {
                // 没时间写了
            }

            else if (cmd[0].trim().equals("close")) {
                // 没时间写了
            } else if (cmd[0].trim().equals("rename")) {

                // System.out.println("文件" + file[0] + "已经关闭");
                if (rename(cmd)) {
                    System.out.println("重命名成功！");
                } else {
                    System.out.println("重命名失败！");
                }

            }
            // read操作（文件已经打开的话可移执行文件的读操作，如果文件没有打开，则可以执行文件的读操作否则不可以）
            else if (cmd[0].trim().equals("read")) {

                Object o = this.getFileByName(cmd[1]);
                if (null != o) {
                    if (o instanceof MyDirectory) {
                        MyDirectory o1 = (MyDirectory) o;
                        System.out.println(o1.getName() + "目录不能执行此命令！");
                    } else if (o instanceof MyFile) {

                        MyFile o1 = (MyFile) o;
                        System.out.println(o1.getName() + "文件内容如下：");
                        System.out.println(o1.getSubstance().substring(0,
                                o1.getSubstance().lastIndexOf("\r\n")));
                    }
                }
            } else if (cmd[0].trim().equals("write")) {

                Object o = this.getFileByName(cmd[1]);
                if (null != o) {
                    if (o instanceof MyDirectory) {
                        MyDirectory o1 = (MyDirectory) o;
                        System.out.println(o1.getName() + "目录不能执行此命令！");
                    } else if (o instanceof MyFile) {
                        MyFile o1 = (MyFile) o;
                        // System.out.println(o1.getName());
                        System.out.println("1.续写;2.重写; 请选择");
                        String select = sc.next();
                        while (true) {

                            if ("1".equals(select)) {
                                System.out.println("请输入续写的数据，以#end结束");
                                StringBuffer content = new StringBuffer(o1
                                        .getSubstance().substring(
                                                0,
                                                o1.getSubstance().lastIndexOf(
                                                        "\r\n")));
                                while (true) {
                                    String tem = sc.next();
                                    if (tem.equals("#end")) {
                                        System.out.println("文件输入结束");
                                        break;// 文件输入结束
                                    }
                                    content.append(tem + "\r\n");
                                }
                                o1.setSubstance(content.toString());
                                System.out.println("续写操作成功！");
                                break;

                            } else if ("2".equals(select)) {
                                System.out.println("请输入重写的数据，以#end结束");
                                StringBuffer content = new StringBuffer();
                                while (true) {
                                    String tem = sc.next();
                                    if (tem.equals("#end")) {
                                        System.out.println("文件输入结束");
                                        break;// 文件输入结束
                                    }
                                    content.append(tem + "\r\n");
                                }
                                o1.setSubstance(content.toString());
                                System.out.println("重写操作成功！");
                                break;

                            } else {
                                System.out.println("输入错误，请重新输入！");
                                select = sc.next();
                            }
                        }
                    }
                } else {
                    System.out.println("输入错误，请重新输入！");

                }
            }
            // 退出操作---保存数据
            else if (cmd[0].trim().equals("exit")) {

                System.exit(0);
            }
            // help操作
            else if (cmd[0].trim().equals("help")) {
                help();

            } else {
                System.out.println(commond);
                System.out.println("错误命令，请输入help命令进行参考");
            }

        }

    }

    /**
     * regeist(String name) 注册用户
     *
     * @param name
     */
    public boolean regeist(String name) {
        int inode_free_index = this.getFreeInode();
        if (inode_free_index > -1) {
            now_inode = inodes[inode_free_index];
            now_inode.setAddress(inode_free_index);// 文件快的地址
            now_inode.setModifytime();
            now_inode.setRight(1);
            now_inode.setState("close");
            now_inode.setType(0);
            now_inode.setUsers(name);
            now_inode.setPath(name + "->");
            now_inode.setMe(inode_free_index);// 当前Inode的索引
            inodes[inode_free_index] = now_inode;
            MyDirectory block = new MyDirectory();
            block.setName(name);
            blocks[inode_free_index] = block;
            users.add(name);
            FileTools.write("f:\\users.dat", users);
            FileTools.write("f:\\inodes.dat", inodes);
            return true;
        }

        return false;
    }

    public void help() {
        System.out.println();
        System.out.print("create ");
        System.out.println("创建文件");
        System.out.print("dir    ");
        System.out.println("列目录文件");
        System.out.print("exit   ");
        System.out.println("退出");
        System.out.println("以下命令需加文件名");
        System.out.println("eg：open ***");
        System.out.print("open   ");
        System.out.println("打开文件");
        System.out.print("close  ");
        System.out.println("关闭文件");
        System.out.print("read   ");
        System.out.println("读文件");
        System.out.print("write  ");
        System.out.println("写文件");
        System.out.print("delete ");
        System.out.println("删除文件");
        System.out.println();
    }

    private Object getFileByName(String name) {
        for (Object o : blocks) {
            if (o instanceof MyDirectory) {
                MyDirectory o1 = (MyDirectory) o;
                if (o1.getName().equals(name)) {
                    return o1;
                }
            } else if (o instanceof MyFile) {
                MyFile o1 = (MyFile) o;
                if (o1.getName().equals(name)) {
                    return o1;
                }
            }
        }
        return null;

    }

    /**
     * isInNames(String name) 判断用户名是否存在
     *
     * @param name
     * @return
     */
    private boolean isInNames(String name) {
        for (String n : users) {
            if (n.equals(name))
                return true;
        }
        return false;
    }

    /**
     * getFreeInode() 得到空的inode
     *
     * @return
     */
    private int getFreeInode() {

        return sb.getInode_free();
    }

    /**
     * getInode(String path) 由path得到Inode
     *
     * @param name
     * @return
     */
    private INode getInode(String path) {
        for (int i = 0; i < 100; i++) {
            if (path.equals(inodes[i].getPath())) {
                return inodes[i];
            }
        }
        return null;
    }

    /**
     * getBlock() 得到空闲的block的序号
     *
     * @param name
     * @return
     */
    private int getBlock() {
        for (int i = 0; i < 100; i++) {
            if (null == blocks[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * rename(String[] cmd) 重命名函数
     *
     * @param cmd
     * @return
     */
    private boolean rename(String[] cmd) {
        if (cmd.length < 3) {
            System.out.println("命令输入错误！");
            return false;
        }
        Object o = getFileByName(cmd[1]);
        if (null == o)
            return false;
        else {
            if (o instanceof MyDirectory) {
                MyDirectory oo = (MyDirectory) o;
                oo.setName(cmd[2]);
                // inode.setPath(now_inode.getPath() + cmd[1] + "->");
                inodes[oo.getInode_address()].setPath(now_inode.getPath()
                        + cmd[2] + "->");
                return true;
            } else {
                MyFile oo = (MyFile) o;
                oo.setName(cmd[2]);
                return true;
            }
        }

    }
}
