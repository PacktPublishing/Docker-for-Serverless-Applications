package main

import (
	"os"
	"os/user"
	"runtime"
	"strconv"

	"golang.org/x/sys/unix"

	"github.com/Sirupsen/logrus"
	"github.com/docker/docker/pkg/namesgenerator"
	"github.com/opencontainers/runc/libcontainer"
	"github.com/opencontainers/runc/libcontainer/configs"
	_ "github.com/opencontainers/runc/libcontainer/nsenter"
)

func init() {
	if len(os.Args) > 1 && os.Args[1] == "init" {
		runtime.GOMAXPROCS(1)
		runtime.LockOSThread()
		factory, _ := libcontainer.New("")
		if err := factory.StartInitialization(); err != nil {
			logrus.Fatal(err)
		}
		panic("--this line should have never been executed, congratulations--")
	}

	logrus.SetLevel(logrus.DebugLevel)
}

func main() {

	containerId := namesgenerator.GetRandomName(0)

	factory, err := libcontainer.New("/tmp/runf",
		libcontainer.Cgroupfs,
		libcontainer.InitArgs(os.Args[0], "init"))
	if err != nil {
		logrus.Fatal(err)
		return
	}

	defaultMountFlags := unix.MS_NOEXEC | unix.MS_NOSUID | unix.MS_NODEV

	cwd, err := os.Getwd()
	currentUser, err := user.Current()
	uid, err := strconv.Atoi(currentUser.Uid)
	gid, err := strconv.Atoi(currentUser.Gid)
	caps := []string{
		"CAP_AUDIT_WRITE",
		"CAP_KILL",
		"CAP_NET_BIND_SERVICE",
	}

	config := &configs.Config{
		Rootfs:          cwd + "/rootfs",
		Readonlyfs:      true,
		NoNewPrivileges: true,
		Rootless:        true,
		Capabilities: &configs.Capabilities{
			Bounding:    caps,
			Permitted:   caps,
			Inheritable: caps,
			Ambient:     caps,
			Effective:   caps,
		},
		Namespaces: configs.Namespaces([]configs.Namespace{
			{Type: configs.NEWNS},
			{Type: configs.NEWUTS},
			{Type: configs.NEWIPC},
			{Type: configs.NEWPID},
			{Type: configs.NEWUSER},
		}),
		Cgroups: &configs.Cgroup{
			Name:   "runf",
			Parent: "system",
			Resources: &configs.Resources{
				MemorySwappiness: nil,
				AllowAllDevices:  nil,
				AllowedDevices:   configs.DefaultAllowedDevices,
			},
		},
		MaskPaths: []string{
			"/proc/kcore",
			"/proc/latency_stats",
			"/proc/timer_list",
			"/proc/timer_stats",
			"/proc/sched_debug",
			"/sys/firmware",
			"/proc/scsi",
		},
		ReadonlyPaths: []string{
			"/proc/asound",
			"/proc/bus",
			"/proc/fs",
			"/proc/irq",
			"/proc/sys",
			"/proc/sysrq-trigger",
		},
		Devices:  configs.DefaultAutoCreatedDevices,
		Hostname: containerId,
		Mounts: []*configs.Mount{
			{
				Source:      "proc",
				Destination: "/proc",
				Device:      "proc",
				Flags:       defaultMountFlags,
			},
			{
				Source:      "tmpfs",
				Destination: "/dev",
				Device:      "tmpfs",
				Flags:       unix.MS_NOSUID | unix.MS_STRICTATIME,
				Data:        "mode=755",
			},
			{
				Device:      "devpts",
				Source:      "devpts",
				Destination: "/dev/pts",
				Flags:       unix.MS_NOSUID | unix.MS_NOEXEC,
				Data:        "newinstance,ptmxmode=0666,mode=0620",
			},
			{
				Device:      "tmpfs",
				Source:      "shm",
				Destination: "/dev/shm",
				Flags:       defaultMountFlags,
				Data:        "mode=1777,size=65536k",
			},
		},
		Rlimits: []configs.Rlimit{
			{
				Type: unix.RLIMIT_NOFILE,
				Hard: uint64(1024),
				Soft: uint64(1024),
			},
		},
		UidMappings: []configs.IDMap{
			{
				ContainerID: 0,
				HostID:      uid,
				Size:        1,
			},
		},
		GidMappings: []configs.IDMap{
			{
				ContainerID: 0,
				HostID:      gid,
				Size:        1,
			},
		},
	}

	container, err := factory.Create(containerId, config)
	if err != nil {
		logrus.Fatal(err)
		return
	}

	environmentVars := []string{
		"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
		"HOSTNAME=" + containerId,
		"TERM=xterm",
	}
	process := &libcontainer.Process{
		Args:   os.Args[1:],
		Env:    environmentVars,
		User:   "root",
		Cwd:    "/",
		Stdin:  os.Stdin,
		Stdout: os.Stdout,
		Stderr: os.Stderr,
	}

	err = container.Run(process)
	if err != nil {
		container.Destroy()
		logrus.Fatal(err)
		return
	}

	_, err = process.Wait()
	if err != nil {
		logrus.Fatal(err)
	}

	defer container.Destroy()
}
